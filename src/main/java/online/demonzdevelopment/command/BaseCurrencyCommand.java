package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.CurrencyRequest;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.rank.Rank;
import online.demonzdevelopment.util.ColorUtil;
import online.demonzdevelopment.util.MessagesUtil;
import online.demonzdevelopment.util.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Base currency command handler with all common currency operations
 */
public abstract class BaseCurrencyCommand implements CommandExecutor, TabCompleter {
    
    protected final DZEconomy plugin;
    protected final CurrencyType currencyType;
    protected final MessagesUtil messageUtil;
    
    public BaseCurrencyCommand(DZEconomy plugin, CurrencyType currencyType) {
        this.plugin = plugin;
        this.currencyType = currencyType;
        this.messageUtil = new MessagesUtil(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                handleBalance(sender, new String[]{});
            } else {
                showHelp(sender);
            }
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "balance":
            case "bal":
                handleBalance(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "send":
            case "pay":
                handleSend(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "request":
            case "req":
                handleRequest(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "accept":
                handleAccept(sender);
                break;
            case "deny":
                handleDeny(sender);
                break;
            case "add":
                handleAdd(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "help":
            default:
                showHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * Handle balance command
     */
    private void handleBalance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage(ColorUtil.translate("&cYou must specify a player from console!"));
            return;
        }
        
        if (args.length == 0) {
            // Show own balance
            Player player = (Player) sender;
            
            if (!player.hasPermission("dzeconomy." + currencyType.getId() + ".balance")) {
                sendMessage(sender, "general.no-permission", null);
                return;
            }
            
            String formatted = plugin.getCurrencyManager().getFormattedBalance(player.getUniqueId(), currencyType);
            String displayName = plugin.getCurrencyManager().getCurrencyDisplayName(currencyType);
            
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "currency", ColorUtil.strip(displayName),
                    "color", currencyType.getColor(),
                    "symbol", plugin.getCurrencyManager().getCurrencySymbol(currencyType),
                    "amount", formatted);
            
            sendMessage(sender, "balance.own", placeholders);
        } else {
            // Show other player's balance
            if (!sender.hasPermission("dzeconomy." + currencyType.getId() + ".balance.others")) {
                sendMessage(sender, "general.no-permission", null);
                return;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                Map<String, String> placeholders = MessagesUtil.placeholders("player", args[0]);
                sendMessage(sender, "general.player-not-found", placeholders);
                return;
            }
            
            String formatted = plugin.getCurrencyManager().getFormattedBalance(target.getUniqueId(), currencyType);
            String displayName = plugin.getCurrencyManager().getCurrencyDisplayName(currencyType);
            
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "player", target.getName(),
                    "currency", ColorUtil.strip(displayName),
                    "color", currencyType.getColor(),
                    "symbol", plugin.getCurrencyManager().getCurrencySymbol(currencyType),
                    "amount", formatted);
            
            sendMessage(sender, "balance.other", placeholders);
        }
    }
    
    /**
     * Handle send command with full validation chain
     */
    private void handleSend(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.translate("&cOnly players can send currency!"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("dzeconomy." + currencyType.getId() + ".send")) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /" + currencyType.getId() + " send <player> <amount>"));
            return;
        }
        
        // VALIDATION CHAIN (strict order as specified)
        
        // 1. Verify both players exist via UUID
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            Map<String, String> placeholders = MessagesUtil.placeholders("player", args[0]);
            sendMessage(sender, "general.player-not-found", placeholders);
            return;
        }
        
        // 2. Prevent self-send
        if (player.getUniqueId().equals(target.getUniqueId())) {
            sendMessage(sender, "general.cannot-afford-self", null);
            return;
        }
        
        // 3. Validate amount > 0
        double amount;
        try {
            amount = NumberFormatter.parse(args[1]);
            if (amount <= 0) {
                sendMessage(sender, "general.invalid-amount", null);
                return;
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "general.invalid-amount", null);
            return;
        }
        
        // Check max transaction limit
        double maxTransaction = plugin.getConfigManager().getConfig().getDouble("limits.max-transaction");
        if (amount > maxTransaction) {
            sendMessage(sender, "general.max-transaction-exceeded", null);
            return;
        }
        
        // 4. Check sender has amount in balance
        double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), currencyType);
        if (balance < amount) {
            String symbol = plugin.getCurrencyManager().getCurrencySymbol(currencyType);
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "currency", currencyType.getName(),
                    "symbol", symbol,
                    "balance", NumberFormatter.formatShort(balance),
                    "amount", NumberFormatter.formatShort(amount));
            sendMessage(sender, "general.insufficient-funds", placeholders);
            return;
        }
        
        // 5. Calculate tax
        Rank senderRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        Rank.RankCurrencySettings settings = senderRank.getSettingsFor(currencyType);
        double taxPercentage = settings.getTransferTax();
        double tax = NumberFormatter.truncateDecimal(amount * (taxPercentage / 100.0));
        double total = NumberFormatter.truncateDecimal(amount + tax);
        
        // 6. Check sender has amount + tax
        if (balance < total) {
            String symbol = plugin.getCurrencyManager().getCurrencySymbol(currencyType);
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "currency", currencyType.getName(),
                    "symbol", symbol,
                    "balance", NumberFormatter.formatShort(balance),
                    "amount", NumberFormatter.formatShort(amount),
                    "tax", NumberFormatter.formatShort(tax),
                    "total", NumberFormatter.formatShort(total));
            sendMessage(sender, "general.insufficient-funds-tax", placeholders);
            return;
        }
        
        // 7. Verify daily send limit not exceeded
        PlayerData senderData = plugin.getCurrencyManager().getPlayerData(player.getUniqueId());
        int dailySendCount = senderData.getDailySendCount(currencyType);
        int dailyLimit = settings.getDailyTransferLimit();
        
        if (dailySendCount >= dailyLimit) {
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "action", "send",
                    "limit", String.valueOf(dailyLimit));
            sendMessage(sender, "general.daily-limit-reached", placeholders);
            return;
        }
        
        // 8. Verify cooldown expired
        int cooldownSeconds = settings.getTransferCooldown();
        if (senderData.isSendCooldownActive(currencyType, cooldownSeconds)) {
            long remaining = senderData.getSendCooldownRemaining(currencyType, cooldownSeconds);
            Map<String, String> placeholders = MessagesUtil.placeholders("cooldown", String.valueOf(remaining));
            sendMessage(sender, "general.cooldown-active", placeholders);
            return;
        }
        
        // EXECUTION
        
        // Deduct from sender
        plugin.getCurrencyManager().removeBalance(player.getUniqueId(), currencyType, total);
        
        // Add to receiver
        plugin.getCurrencyManager().addBalance(target.getUniqueId(), currencyType, amount);
        
        // Update statistics
        senderData.addMoneySent(currencyType, amount);
        senderData.incrementDailySendCount(currencyType);
        senderData.setSendCooldown(currencyType, System.currentTimeMillis());
        
        PlayerData targetData = plugin.getCurrencyManager().getPlayerData(target.getUniqueId());
        targetData.addMoneyReceived(currencyType, amount);
        
        // Save data
        plugin.getCurrencyManager().savePlayerDataAsync(player.getUniqueId());
        plugin.getCurrencyManager().savePlayerDataAsync(target.getUniqueId());
        
        // NOTIFICATIONS
        String symbol = plugin.getCurrencyManager().getCurrencySymbol(currencyType);
        Map<String, String> placeholders = MessagesUtil.placeholders(
                "player", target.getName(),
                "symbol", symbol,
                "amount", NumberFormatter.formatShort(amount),
                "tax", NumberFormatter.formatShort(tax),
                "currency", currencyType.getName());
        
        sendMessage(sender, "send.success-sender", placeholders);
        sendMessage(sender, "send.balance-update", MessagesUtil.placeholders(
                "symbol", symbol,
                "balance", NumberFormatter.formatShort(plugin.getCurrencyManager().getBalance(player.getUniqueId(), currencyType)),
                "color", currencyType.getColor()));
        
        if (target.isOnline()) {
            Player targetPlayer = target.getPlayer();
            Map<String, String> targetPlaceholders = MessagesUtil.placeholders(
                    "player", player.getName(),
                    "symbol", symbol,
                    "amount", NumberFormatter.formatShort(amount),
                    "currency", currencyType.getName());
            
            sendMessage(targetPlayer, "send.success-receiver", targetPlaceholders);
            sendMessage(targetPlayer, "send.balance-update", MessagesUtil.placeholders(
                    "symbol", symbol,
                    "balance", NumberFormatter.formatShort(plugin.getCurrencyManager().getBalance(target.getUniqueId(), currencyType)),
                    "color", currencyType.getColor()));
            
            targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
    
    /**
     * Handle request command with full validation
     */
    private void handleRequest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.translate("&cOnly players can request currency!"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("dzeconomy." + currencyType.getId() + ".request")) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /" + currencyType.getId() + " request <player> <amount>"));
            return;
        }
        
        // VALIDATION CHAIN
        
        // 1. Verify both players exist and ONLINE
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            Map<String, String> placeholders = MessagesUtil.placeholders("player", args[0]);
            sendMessage(sender, "general.player-offline", placeholders);
            return;
        }
        
        // 2. Prevent self-request
        if (player.getUniqueId().equals(target.getUniqueId())) {
            sendMessage(sender, "request.cannot-request-self", null);
            return;
        }
        
        // 3. Validate amount > 0
        double amount;
        try {
            amount = NumberFormatter.parse(args[1]);
            if (amount <= 0) {
                sendMessage(sender, "general.invalid-amount", null);
                return;
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "general.invalid-amount", null);
            return;
        }
        
        // 4. Check no duplicate pending request
        if (plugin.getCurrencyManager().hasPendingRequestWith(player.getUniqueId(), target.getUniqueId())) {
            sendMessage(sender, "request.already-pending", null);
            return;
        }
        
        // 5. Verify daily request limit not exceeded
        PlayerData requesterData = plugin.getCurrencyManager().getPlayerData(player.getUniqueId());
        Rank requesterRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        Rank.RankCurrencySettings settings = requesterRank.getSettingsFor(currencyType);
        
        int dailyRequestCount = requesterData.getDailyRequestCount(currencyType);
        int dailyLimit = settings.getDailyRequestLimit();
        
        if (dailyRequestCount >= dailyLimit) {
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "action", "request",
                    "limit", String.valueOf(dailyLimit));
            sendMessage(sender, "general.daily-limit-reached", placeholders);
            return;
        }
        
        // 6. Verify request cooldown expired
        int cooldownSeconds = settings.getRequestCooldown();
        if (requesterData.isRequestCooldownActive(currencyType, cooldownSeconds)) {
            long remaining = requesterData.getRequestCooldownRemaining(currencyType, cooldownSeconds);
            Map<String, String> placeholders = MessagesUtil.placeholders("cooldown", String.valueOf(remaining));
            sendMessage(sender, "general.cooldown-active", placeholders);
            return;
        }
        
        // CREATE REQUEST
        CurrencyRequest request = new CurrencyRequest(player.getUniqueId(), target.getUniqueId(), currencyType, amount);
        plugin.getCurrencyManager().addRequest(request);
        
        // Update statistics
        requesterData.incrementDailyRequestCount(currencyType);
        plugin.getCurrencyManager().savePlayerDataAsync(player.getUniqueId());
        
        // NOTIFICATIONS
        String symbol = plugin.getCurrencyManager().getCurrencySymbol(currencyType);
        Map<String, String> placeholders = MessagesUtil.placeholders(
                "player", target.getName(),
                "symbol", symbol,
                "amount", NumberFormatter.formatShort(amount),
                "currency", currencyType.getName());
        
        sendMessage(sender, "request.sent", placeholders);
        
        // Send chat notification to target
        Map<String, String> targetPlaceholders = MessagesUtil.placeholders(
                "player", player.getName(),
                "symbol", symbol,
                "amount", NumberFormatter.formatShort(amount),
                "currency", currencyType.getName());
        
        sendMessage(target, "request.received-chat", targetPlaceholders);
        
        // Open GUI if enabled and no inventory open
        if (plugin.getConfigManager().getConfig().getBoolean("gui.request.enabled", true)) {
            plugin.getRequestGUIManager().openRequestGUI(target, request);
        }
    }
    
    /**
     * Handle accept command with full validation
     */
    private void handleAccept(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.translate("&cOnly players can accept requests!"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("dzeconomy." + currencyType.getId() + ".accept")) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        // Get pending request
        CurrencyRequest request = plugin.getCurrencyManager().getPendingRequest(player.getUniqueId());
        
        if (request == null) {
            sendMessage(sender, "request.no-pending", null);
            return;
        }
        
        // Check currency type matches
        if (request.getCurrency() != currencyType) {
            sendMessage(sender, "request.no-pending", null);
            return;
        }
        
        // Check not expired
        int timeoutSeconds = plugin.getConfigManager().getConfig().getInt("limits.request-timeout", 120);
        if (request.isExpired(timeoutSeconds)) {
            plugin.getCurrencyManager().removeRequest(player.getUniqueId());
            sendMessage(sender, "request.expired", null);
            return;
        }
        
        // Check requester still online
        Player requester = Bukkit.getPlayer(request.getRequesterUUID());
        if (requester == null || !requester.isOnline()) {
            plugin.getCurrencyManager().removeRequest(player.getUniqueId());
            Map<String, String> placeholders = MessagesUtil.placeholders("player", Bukkit.getOfflinePlayer(request.getRequesterUUID()).getName());
            sendMessage(sender, "general.player-offline", placeholders);
            return;
        }
        
        double amount = request.getAmount();
        
        // Check accepter has amount
        double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), currencyType);
        if (balance < amount) {
            String symbol = plugin.getCurrencyManager().getCurrencySymbol(currencyType);
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "currency", currencyType.getName(),
                    "symbol", symbol,
                    "balance", NumberFormatter.formatShort(balance),
                    "amount", NumberFormatter.formatShort(amount));
            sendMessage(sender, "general.insufficient-funds", placeholders);
            return;
        }
        
        // Calculate tax on accepter's rank
        Rank accepterRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        Rank.RankCurrencySettings settings = accepterRank.getSettingsFor(currencyType);
        double taxPercentage = settings.getTransferTax();
        double tax = NumberFormatter.truncateDecimal(amount * (taxPercentage / 100.0));
        double total = NumberFormatter.truncateDecimal(amount + tax);
        
        // Check accepter has amount + tax
        if (balance < total) {
            String symbol = plugin.getCurrencyManager().getCurrencySymbol(currencyType);
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "currency", currencyType.getName(),
                    "symbol", symbol,
                    "balance", NumberFormatter.formatShort(balance),
                    "amount", NumberFormatter.formatShort(amount),
                    "tax", NumberFormatter.formatShort(tax),
                    "total", NumberFormatter.formatShort(total));
            sendMessage(sender, "general.insufficient-funds-tax", placeholders);
            return;
        }
        
        // Verify accepter's daily send limit
        PlayerData accepterData = plugin.getCurrencyManager().getPlayerData(player.getUniqueId());
        int dailySendCount = accepterData.getDailySendCount(currencyType);
        int dailyLimit = settings.getDailyTransferLimit();
        
        if (dailySendCount >= dailyLimit) {
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "action", "send",
                    "limit", String.valueOf(dailyLimit));
            sendMessage(sender, "general.daily-limit-reached", placeholders);
            return;
        }
        
        // Verify accepter's send cooldown
        int cooldownSeconds = settings.getTransferCooldown();
        if (accepterData.isSendCooldownActive(currencyType, cooldownSeconds)) {
            long remaining = accepterData.getSendCooldownRemaining(currencyType, cooldownSeconds);
            Map<String, String> placeholders = MessagesUtil.placeholders("cooldown", String.valueOf(remaining));
            sendMessage(sender, "general.cooldown-active", placeholders);
            return;
        }
        
        // EXECUTION
        
        // Deduct from accepter
        plugin.getCurrencyManager().removeBalance(player.getUniqueId(), currencyType, total);
        
        // Add to requester
        plugin.getCurrencyManager().addBalance(requester.getUniqueId(), currencyType, amount);
        
        // Update statistics
        accepterData.addMoneySent(currencyType, amount);
        accepterData.incrementDailySendCount(currencyType);
        accepterData.setSendCooldown(currencyType, System.currentTimeMillis());
        
        PlayerData requesterData = plugin.getCurrencyManager().getPlayerData(requester.getUniqueId());
        requesterData.addMoneyReceived(currencyType, amount);
        requesterData.setRequestCooldown(currencyType, System.currentTimeMillis());
        
        // Remove request
        plugin.getCurrencyManager().removeRequest(player.getUniqueId());
        
        // Save data
        plugin.getCurrencyManager().savePlayerDataAsync(player.getUniqueId());
        plugin.getCurrencyManager().savePlayerDataAsync(requester.getUniqueId());
        
        // Close GUI if open
        plugin.getRequestGUIManager().closeGUI(player);
        
        // NOTIFICATIONS
        String symbol = plugin.getCurrencyManager().getCurrencySymbol(currencyType);
        Map<String, String> placeholders = MessagesUtil.placeholders(
                "player", requester.getName(),
                "symbol", symbol,
                "amount", NumberFormatter.formatShort(amount),
                "tax", NumberFormatter.formatShort(tax));
        
        sendMessage(sender, "request.accepted-accepter", placeholders);
        sendMessage(sender, "send.balance-update", MessagesUtil.placeholders(
                "symbol", symbol,
                "balance", NumberFormatter.formatShort(plugin.getCurrencyManager().getBalance(player.getUniqueId(), currencyType)),
                "color", currencyType.getColor()));
        
        Map<String, String> requesterPlaceholders = MessagesUtil.placeholders(
                "player", player.getName(),
                "symbol", symbol,
                "amount", NumberFormatter.formatShort(amount));
        
        sendMessage(requester, "request.accepted-requester", requesterPlaceholders);
        sendMessage(requester, "send.balance-update", MessagesUtil.placeholders(
                "symbol", symbol,
                "balance", NumberFormatter.formatShort(plugin.getCurrencyManager().getBalance(requester.getUniqueId(), currencyType)),
                "color", currencyType.getColor()));
        
        requester.playSound(requester.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
    
    /**
     * Handle deny command
     */
    private void handleDeny(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.translate("&cOnly players can deny requests!"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("dzeconomy." + currencyType.getId() + ".deny")) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        CurrencyRequest request = plugin.getCurrencyManager().getPendingRequest(player.getUniqueId());
        
        if (request == null || request.getCurrency() != currencyType) {
            sendMessage(sender, "request.no-pending", null);
            return;
        }
        
        Player requester = Bukkit.getPlayer(request.getRequesterUUID());
        
        plugin.getCurrencyManager().removeRequest(player.getUniqueId());
        plugin.getRequestGUIManager().closeGUI(player);
        
        sendMessage(sender, "request.denied-denier", null);
        
        if (requester != null && requester.isOnline()) {
            Map<String, String> placeholders = MessagesUtil.placeholders("player", player.getName());
            sendMessage(requester, "request.denied-requester", placeholders);
        }
        
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
    }
    
    /**
     * Handle admin add command
     */
    private void handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dzeconomy.admin." + currencyType.getId() + ".add") && !sender.isOp()) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /" + currencyType.getId() + " add <player> <amount>"));
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            Map<String, String> placeholders = MessagesUtil.placeholders("player", args[0]);
            sendMessage(sender, "general.player-not-found", placeholders);
            return;
        }
        
        double amount;
        try {
            amount = NumberFormatter.parse(args[1]);
            if (amount <= 0) {
                sendMessage(sender, "general.invalid-amount", null);
                return;
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "general.invalid-amount", null);
            return;
        }
        
        plugin.getCurrencyManager().addBalance(target.getUniqueId(), currencyType, amount);
        
        String symbol = plugin.getCurrencyManager().getCurrencySymbol(currencyType);
        double newBalance = plugin.getCurrencyManager().getBalance(target.getUniqueId(), currencyType);
        
        Map<String, String> placeholders = MessagesUtil.placeholders(
                "player", target.getName(),
                "symbol", symbol,
                "amount", NumberFormatter.formatShort(amount),
                "balance", NumberFormatter.formatShort(newBalance),
                "currency", currencyType.getName());
        
        sendMessage(sender, "admin.add-success", placeholders);
        
        if (target.isOnline()) {
            Player targetPlayer = target.getPlayer();
            sendMessage(targetPlayer, "admin.add-notify", placeholders);
            sendMessage(targetPlayer, "send.balance-update", MessagesUtil.placeholders(
                    "symbol", symbol,
                    "balance", NumberFormatter.formatShort(newBalance),
                    "color", currencyType.getColor()));
        }
    }
    
    /**
     * Show help menu
     */
    private void showHelp(CommandSender sender) {
        boolean isAdmin = sender.hasPermission("dzeconomy.admin") || sender.isOp();
        
        String helpMessage = messageUtil.getMessage("help." + currencyType.getId());
        
        if (isAdmin) {
            String adminCommands = messageUtil.getMessage("help." + currencyType.getId() + "-admin");
            helpMessage = helpMessage.replace("{admin_commands}", adminCommands);
        } else {
            helpMessage = helpMessage.replace("{admin_commands}", "");
        }
        
        sender.sendMessage(helpMessage);
    }
    
    /**
     * Send a message with placeholders
     */
    protected void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = messageUtil.getMessage(path, placeholders);
        sender.sendMessage(message);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("balance");
            completions.add("send");
            completions.add("request");
            completions.add("accept");
            completions.add("deny");
            completions.add("help");
            
            if (sender.hasPermission("dzeconomy.admin") || sender.isOp()) {
                completions.add("add");
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("send") || 
                args[0].equalsIgnoreCase("request") || args[0].equalsIgnoreCase("add") ||
                args[0].equalsIgnoreCase("balance"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("send") || 
                args[0].equalsIgnoreCase("request") || args[0].equalsIgnoreCase("add"))) {
            completions.add("100");
            completions.add("1000");
            completions.add("10000");
        }
        
        return completions;
    }
}