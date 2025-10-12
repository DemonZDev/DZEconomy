package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.request.PaymentRequest;
import online.demonzdevelopment.util.FormatUtil;
import online.demonzdevelopment.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;

public abstract class AbstractCurrencyCommand implements CommandExecutor, TabCompleter {
    protected final DZEconomy plugin;
    protected final CurrencyType currencyType;

    public AbstractCurrencyCommand(DZEconomy plugin, CurrencyType currencyType) {
        this.plugin = plugin;
        this.currencyType = currencyType;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("general.must-be-online"));
            return true;
        }
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "balance", "bal" -> handleBalance(player, args);
            case "send", "pay" -> handleSend(player, args);
            case "request", "req" -> handleRequest(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "add" -> handleAdd(player, args);
            case "help" -> showHelp(player);
            default -> showHelp(player);
        }
        
        return true;
    }

    private void handleBalance(Player player, String[] args) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.player-not-found"));
            return;
        }
        
        BigDecimal balance = data.getBalance(currencyType);
        String formatted = formatAmount(balance);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", formatted);
        
        player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".balance", placeholders));
    }

    private void handleSend(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".help.send"));
            return;
        }
        
        String targetName = args[1];
        BigDecimal amount = NumberUtil.parse(args[2]);
        
        if (amount == null || !NumberUtil.isPositive(amount)) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.invalid-amount"));
            return;
        }
        
        plugin.getPlayerDataManager().getUUIDByName(targetName).thenAccept(targetUUID -> {
            if (targetUUID == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.player-not-found"));
                return;
            }
            
            if (targetUUID.equals(player.getUniqueId())) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.cannot-send-self"));
                return;
            }
            
            executeSend(player, targetUUID, targetName, amount);
        });
    }

    private void executeSend(Player sender, UUID targetUUID, String targetName, BigDecimal amount) {
        PlayerData senderData = plugin.getPlayerDataManager().getPlayerData(sender.getUniqueId());
        if (senderData == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("general.player-not-found"));
            return;
        }
        
        if (!plugin.getEconomyManager().isAmountValid(sender, currencyType, amount)) {
            BigDecimal min = plugin.getEconomyManager().getMinSendLimit(sender, currencyType);
            BigDecimal max = plugin.getEconomyManager().getMaxSendLimit(sender, currencyType);
            
            if (amount.compareTo(min) < 0) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("min", formatAmount(min));
                sender.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".send.amount-too-low", placeholders));
            } else {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("max", formatAmount(max));
                sender.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".send.amount-too-high", placeholders));
            }
            return;
        }
        
        if (!plugin.getEconomyManager().canSend(sender, currencyType)) {
            long remaining = plugin.getEconomyManager().getCooldownRemaining(sender, currencyType);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", FormatUtil.formatTime(remaining));
            sender.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".send.cooldown", placeholders));
            return;
        }
        
        if (plugin.getEconomyManager().hasReachedSendLimit(sender, currencyType)) {
            int limit = plugin.getEconomyManager().getTimesSendLimit(sender, currencyType);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("limit", String.valueOf(limit));
            sender.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".send.limit-reached", placeholders));
            return;
        }
        
        BigDecimal tax = plugin.getEconomyManager().calculateTax(sender, currencyType, amount);
        BigDecimal total = amount.add(tax);
        
        if (!senderData.hasBalance(currencyType, total)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", formatAmount(total));
            placeholders.put("tax", formatAmount(tax));
            sender.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".send.insufficient", placeholders));
            return;
        }
        
        plugin.getPlayerDataManager().loadPlayerData(targetUUID, targetName).thenAccept(targetData -> {
            senderData.subtractBalance(currencyType, total);
            targetData.addBalance(currencyType, amount);
            
            senderData.setLastSendTime(currencyType, System.currentTimeMillis());
            senderData.incrementDailySendCount(currencyType);
            
            Map<String, String> senderPlaceholders = new HashMap<>();
            senderPlaceholders.put("amount", formatAmount(amount));
            senderPlaceholders.put("player", targetName);
            senderPlaceholders.put("tax", formatAmount(tax));
            
            sender.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".send.success", senderPlaceholders));
            
            Player targetPlayer = Bukkit.getPlayer(targetUUID);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                Map<String, String> receiverPlaceholders = new HashMap<>();
                receiverPlaceholders.put("amount", formatAmount(amount));
                receiverPlaceholders.put("player", sender.getName());
                
                targetPlayer.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".send.received", receiverPlaceholders));
            }
        });
    }

    private void handleRequest(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".help.request"));
            return;
        }
        
        String targetName = args[1];
        BigDecimal amount = NumberUtil.parse(args[2]);
        
        if (amount == null || !NumberUtil.isPositive(amount)) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.invalid-amount"));
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.player-not-found"));
            return;
        }
        
        if (target.equals(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.cannot-send-self"));
            return;
        }
        
        plugin.getRequestManager().createRequest(player, target, currencyType, amount);
        
        Map<String, String> requesterPlaceholders = new HashMap<>();
        requesterPlaceholders.put("amount", formatAmount(amount));
        requesterPlaceholders.put("player", target.getName());
        
        Map<String, String> targetPlaceholders = new HashMap<>();
        targetPlaceholders.put("amount", formatAmount(amount));
        targetPlaceholders.put("player", player.getName());
        
        player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".request.sent", requesterPlaceholders));
        target.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".request.received", targetPlaceholders));
    }

    private void handleAccept(Player player) {
        PaymentRequest request = plugin.getRequestManager().getPendingRequest(player.getUniqueId());
        
        if (request == null) {
            player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".request.no-pending"));
            return;
        }
        
        if (request.isExpired()) {
            plugin.getRequestManager().removePendingRequest(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".request.timeout"));
            return;
        }
        
        if (!request.getCurrency().equals(currencyType)) {
            return;
        }
        
        Player requester = Bukkit.getPlayer(request.getRequester());
        if (requester == null || !requester.isOnline()) {
            plugin.getRequestManager().removePendingRequest(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("general.player-not-found"));
            return;
        }
        
        plugin.getRequestManager().removePendingRequest(player.getUniqueId());
        
        executeSend(player, requester.getUniqueId(), requester.getName(), request.getAmount());
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", requester.getName());
        player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".request.accepted-sender", placeholders));
        
        Map<String, String> requesterPlaceholders = new HashMap<>();
        requesterPlaceholders.put("player", player.getName());
        requester.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".request.accepted-receiver", requesterPlaceholders));
    }

    private void handleDeny(Player player) {
        PaymentRequest request = plugin.getRequestManager().getPendingRequest(player.getUniqueId());
        
        if (request == null) {
            player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".request.no-pending"));
            return;
        }
        
        if (!request.getCurrency().equals(currencyType)) {
            return;
        }
        
        Player requester = Bukkit.getPlayer(request.getRequester());
        
        plugin.getRequestManager().removePendingRequest(player.getUniqueId());
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", requester != null ? requester.getName() : "Unknown");
        player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".request.denied-sender", placeholders));
        
        if (requester != null && requester.isOnline()) {
            Map<String, String> requesterPlaceholders = new HashMap<>();
            requesterPlaceholders.put("player", player.getName());
            requester.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".request.denied-receiver", requesterPlaceholders));
        }
    }

    private void handleAdd(Player player, String[] args) {
        if (!player.hasPermission("dzeconomy.add")) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".help.add"));
            return;
        }
        
        String targetName = args[1];
        BigDecimal amount = NumberUtil.parse(args[2]);
        
        if (amount == null || !NumberUtil.isPositive(amount)) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.invalid-amount"));
            return;
        }
        
        plugin.getPlayerDataManager().getUUIDByName(targetName).thenAccept(targetUUID -> {
            if (targetUUID == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.player-not-found"));
                return;
            }
            
            plugin.getPlayerDataManager().loadPlayerData(targetUUID, targetName).thenAccept(targetData -> {
                targetData.addBalance(currencyType, amount);
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("amount", formatAmount(amount));
                placeholders.put("player", targetName);
                
                player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".add.success", placeholders));
                
                Player targetPlayer = Bukkit.getPlayer(targetUUID);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    Map<String, String> receiverPlaceholders = new HashMap<>();
                    receiverPlaceholders.put("amount", formatAmount(amount));
                    
                    targetPlayer.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".add.received", receiverPlaceholders));
                }
            });
        });
    }

    private void showHelp(Player player) {
        player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".help.header"));
        player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".help.balance"));
        player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".help.send"));
        player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".help.request"));
        player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".help.accept"));
        player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".help.deny"));
        
        if (player.hasPermission("dzeconomy.add")) {
            player.sendMessage(plugin.getMessageManager().getMessage(currencyType.getKey() + ".help.add"));
        }
    }

    protected String formatAmount(BigDecimal amount) {
        return FormatUtil.formatCurrency(
            amount,
            plugin.getConfigManager().useShortForm(),
            plugin.getConfigManager().getDecimalLimit()
        );
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("balance", "send", "request", "accept", "deny", "help"));
            if (sender.hasPermission("dzeconomy.add")) {
                completions.add("add");
            }
            return completions;
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("send") || args[0].equalsIgnoreCase("request") || args[0].equalsIgnoreCase("add"))) {
            return null;
        }
        
        return Collections.emptyList();
    }
}