package online.demonzdevelopment.dzeconomy.command;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.currency.CurrencyType;
import online.demonzdevelopment.dzeconomy.data.PlayerData;
import online.demonzdevelopment.dzeconomy.rank.Rank;
import online.demonzdevelopment.dzeconomy.update.UpdateManager;
import online.demonzdevelopment.dzeconomy.util.ColorUtil;
import online.demonzdevelopment.dzeconomy.util.MessagesUtil;
import online.demonzdevelopment.dzeconomy.util.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Economy command for conversion and admin operations
 */
public class EconomyCommand implements CommandExecutor, TabCompleter {
    
    private final DZEconomy plugin;
    private final MessagesUtil messageUtil;
    
    public EconomyCommand(DZEconomy plugin) {
        this.plugin = plugin;
        this.messageUtil = new MessagesUtil(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "convert":
                handleConvert(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "reload":
                handleReload(sender);
                break;
            case "version":
                handleVersion(sender);
                break;
            case "credits":
                handleCredits(sender);
                break;
            case "update":
                handleUpdate(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            default:
                showHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * Handle currency conversion
     */
    private void handleConvert(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.translate("&cOnly players can convert currency!"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("dzeconomy.economy.convert")) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        if (!plugin.getConfigManager().getConfig().getBoolean("conversion.enabled", true)) {
            sendMessage(sender, "conversion.disabled", null);
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(ColorUtil.translate("&cUsage: /economy convert <from> <to> <amount>"));
            player.sendMessage(ColorUtil.translate("&7Example: &e/economy convert money gem 10000"));
            return;
        }
        
        // Parse currencies
        CurrencyType fromCurrency = CurrencyType.fromString(args[0]);
        CurrencyType toCurrency = CurrencyType.fromString(args[1]);
        
        if (fromCurrency == null || toCurrency == null) {
            sendMessage(sender, "general.invalid-currency", null);
            return;
        }
        
        if (fromCurrency == toCurrency) {
            sendMessage(sender, "conversion.same-currency", null);
            return;
        }
        
        // Parse amount
        double amount;
        try {
            amount = NumberFormatter.parse(args[2]);
            if (amount <= 0) {
                sendMessage(sender, "general.invalid-amount", null);
                return;
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "general.invalid-amount", null);
            return;
        }
        
        // Check player has amount
        double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), fromCurrency);
        if (balance < amount) {
            String symbol = plugin.getCurrencyManager().getCurrencySymbol(fromCurrency);
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "currency", fromCurrency.getName(),
                    "symbol", symbol,
                    "balance", NumberFormatter.formatShort(balance),
                    "amount", NumberFormatter.formatShort(amount));
            sendMessage(sender, "general.insufficient-funds", placeholders);
            return;
        }
        
        // Calculate conversion
        double conversionRate = getConversionRate(fromCurrency, toCurrency);
        double convertedAmount = NumberFormatter.truncateDecimal(amount * conversionRate);
        
        // Calculate conversion tax
        Rank playerRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        double conversionTaxPercent = playerRank.getConversionTax();
        double tax = NumberFormatter.truncateDecimal(amount * (conversionTaxPercent / 100.0));
        double total = NumberFormatter.truncateDecimal(amount + tax);
        
        // Check player has amount + tax
        if (balance < total) {
            String symbol = plugin.getCurrencyManager().getCurrencySymbol(fromCurrency);
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "currency", fromCurrency.getName(),
                    "symbol", symbol,
                    "balance", NumberFormatter.formatShort(balance),
                    "amount", NumberFormatter.formatShort(amount),
                    "tax", NumberFormatter.formatShort(tax),
                    "total", NumberFormatter.formatShort(total));
            sendMessage(sender, "general.insufficient-funds-tax", placeholders);
            return;
        }
        
        // Execute conversion
        plugin.getCurrencyManager().removeBalance(player.getUniqueId(), fromCurrency, total);
        plugin.getCurrencyManager().addBalance(player.getUniqueId(), toCurrency, convertedAmount);
        
        // Save data
        plugin.getCurrencyManager().savePlayerDataAsync(player.getUniqueId());
        
        // Send success message
        String symbol1 = plugin.getCurrencyManager().getCurrencySymbol(fromCurrency);
        String symbol2 = plugin.getCurrencyManager().getCurrencySymbol(toCurrency);
        
        Map<String, String> placeholders = MessagesUtil.placeholders(
                "symbol1", symbol1,
                "amount1", NumberFormatter.formatShort(amount),
                "currency1", fromCurrency.getName(),
                "symbol2", symbol2,
                "amount2", NumberFormatter.formatShort(convertedAmount),
                "currency2", toCurrency.getName(),
                "tax", NumberFormatter.formatShort(tax));
        
        sendMessage(sender, "conversion.success", placeholders);
    }
    
    /**
     * Get conversion rate between two currencies
     */
    private double getConversionRate(CurrencyType from, CurrencyType to) {
        // Get base rates from config
        double gemToMobcoin = plugin.getConfigManager().getConfig().getDouble("conversion.rates.gem-to-mobcoin", 100.0);
        double gemToMoney = plugin.getConfigManager().getConfig().getDouble("conversion.rates.gem-to-money", 10000.0);
        double mobcoinToMoney = plugin.getConfigManager().getConfig().getDouble("conversion.rates.mobcoin-to-money", 100.0);
        
        // Calculate all possible conversions
        if (from == CurrencyType.GEM && to == CurrencyType.MOBCOIN) {
            return gemToMobcoin;
        } else if (from == CurrencyType.GEM && to == CurrencyType.MONEY) {
            return gemToMoney;
        } else if (from == CurrencyType.MOBCOIN && to == CurrencyType.GEM) {
            return 1.0 / gemToMobcoin;
        } else if (from == CurrencyType.MOBCOIN && to == CurrencyType.MONEY) {
            return mobcoinToMoney;
        } else if (from == CurrencyType.MONEY && to == CurrencyType.GEM) {
            return 1.0 / gemToMoney;
        } else if (from == CurrencyType.MONEY && to == CurrencyType.MOBCOIN) {
            return 1.0 / mobcoinToMoney;
        }
        
        return 1.0;
    }
    
    /**
     * Handle reload command
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("dzeconomy.admin.reload") && !sender.isOp()) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        plugin.reload();
        sendMessage(sender, "admin.reload-success", null);
    }
    
    /**
     * Handle update command
     */
    private void handleUpdate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dzeconomy.admin.update") && !sender.isOp()) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /economy update <version|previous|next|latest|auto>"));
            sender.sendMessage(ColorUtil.translate("&7Examples:"));
            sender.sendMessage(ColorUtil.translate("&e  /economy update latest &7- Update to latest version"));
            sender.sendMessage(ColorUtil.translate("&e  /economy update 1.2.3 &7- Update to specific version"));
            sender.sendMessage(ColorUtil.translate("&e  /economy update previous &7- Downgrade to previous version"));
            sender.sendMessage(ColorUtil.translate("&e  /economy update next &7- Upgrade to next version"));
            sender.sendMessage(ColorUtil.translate("&e  /economy update auto &7- Auto-update if newer version exists"));
            return;
        }
        
        String updateType = args[0].toLowerCase();
        
        // Execute async to not block main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            online.demonzdevelopment.dzeconomy.update.UpdateManager updateManager = 
                    new online.demonzdevelopment.dzeconomy.update.UpdateManager(plugin);
            
            switch (updateType) {
                case "latest":
                    updateManager.updateToLatest(sender).thenAccept(result -> {
                        logUpdateResult(sender, result);
                    });
                    break;
                    
                case "previous":
                    updateManager.updateToPrevious(sender).thenAccept(result -> {
                        logUpdateResult(sender, result);
                    });
                    break;
                    
                case "next":
                    updateManager.updateToNext(sender).thenAccept(result -> {
                        logUpdateResult(sender, result);
                    });
                    break;
                    
                case "auto":
                    updateManager.autoUpdate().thenAccept(result -> {
                        if (result.isSuccess()) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                sender.sendMessage(ColorUtil.translate(
                                        "&8[&6DZ&eEconomy&8] &a&l✓ Auto-update completed!"));
                                sender.sendMessage(ColorUtil.translate(
                                        "&8[&6DZ&eEconomy&8] &eRestart server to apply changes."));
                            });
                        } else {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                sender.sendMessage(ColorUtil.translate(
                                        "&8[&6DZ&eEconomy&8] &7" + result.getMessage()));
                            });
                        }
                        logUpdateResult(sender, result);
                    });
                    break;
                    
                default:
                    // Treat as version number
                    updateManager.updateToVersion(sender, updateType).thenAccept(result -> {
                        logUpdateResult(sender, result);
                    });
                    break;
            }
        });
    }
    
    /**
     * Log update result
     */
    private void logUpdateResult(CommandSender sender, online.demonzdevelopment.dzeconomy.update.UpdateManager.UpdateResult result) {
        if (result.isSuccess()) {
            plugin.getLogger().info("Update successful: v" + result.getVersion() + " - " + result.getMessage());
        } else {
            plugin.getLogger().info("Update status: " + result.getMessage());
        }
    }
    
    /**
     * Handle credits command - Show plugin creator information
     */
    private void handleCredits(CommandSender sender) {
        if (!sender.hasPermission("dzeconomy.admin") && !sender.isOp()) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        // Hardcoded credits message (not configurable)
        sender.sendMessage(ColorUtil.translate("&a&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        sender.sendMessage(ColorUtil.translate("&6&lCreated by DemonZ Development"));
        sender.sendMessage(ColorUtil.translate("&e"));
        sender.sendMessage(ColorUtil.translate("&6DemonZ Development Ecosystem:"));
        sender.sendMessage(ColorUtil.translate("&f-  &bdemonzdevelopment.online"));
        sender.sendMessage(ColorUtil.translate("&f-  &bhyzerox.me"));
        sender.sendMessage(ColorUtil.translate("&a&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }
    
    /**
     * Handle version/update check command
     */
    private void handleVersion(CommandSender sender) {
        if (!sender.hasPermission("dzeconomy.economy.version") && 
            !sender.hasPermission("dzeconomy.admin") && !sender.isOp()) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        String currentVersion = plugin.getDescription().getVersion();
        sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &7Version: &av" + currentVersion));
        sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &7Author: &eDemonZ Development"));
        
        if (plugin.getUpdateChecker() != null && plugin.getUpdateChecker().isCheckComplete()) {
            if (plugin.getUpdateChecker().isUpdateAvailable()) {
                String latestVersion = plugin.getUpdateChecker().getLatestVersion();
                String downloadUrl = plugin.getUpdateChecker().getDownloadUrl();
                sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &e&lUPDATE AVAILABLE!"));
                sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &7Latest: &av" + latestVersion));
                sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &7Download: &b" + downloadUrl));
            } else {
                sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &a✓ You are running the latest version!"));
            }
        } else {
            sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &7Checking for updates..."));
            // Trigger update check if not done yet
            if (plugin.getUpdateChecker() != null) {
                plugin.getUpdateChecker().checkForUpdates().thenAccept(hasUpdate -> {
                    Bukkit.getScheduler().runTask(plugin, () -> handleVersion(sender));
                });
            }
        }
    }
    
    /**
     * Show help menu
     */
    private void showHelp(CommandSender sender) {
        boolean isAdmin = sender.hasPermission("dzeconomy.admin") || sender.isOp();
        
        String helpMessage = messageUtil.getMessage("help.economy");
        
        if (isAdmin) {
            String adminCommands = messageUtil.getMessage("help.economy-admin");
            helpMessage = helpMessage.replace("{admin_commands}", adminCommands);
        } else {
            helpMessage = helpMessage.replace("{admin_commands}", "");
        }
        
        sender.sendMessage(helpMessage);
    }
    
    /**
     * Send a message with placeholders
     */
    private void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = messageUtil.getMessage(path, placeholders);
        sender.sendMessage(message);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("convert");
            completions.add("version");
            if (sender.hasPermission("dzeconomy.admin") || sender.isOp()) {
                completions.add("reload");
                completions.add("credits");
                completions.add("update");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("update")) {
            if (sender.hasPermission("dzeconomy.admin.update") || sender.isOp()) {
                completions.add("latest");
                completions.add("previous");
                completions.add("next");
                completions.add("auto");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("convert")) {
            completions.add("money");
            completions.add("mobcoin");
            completions.add("gem");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("convert")) {
            completions.add("money");
            completions.add("mobcoin");
            completions.add("gem");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("convert")) {
            completions.add("100");
            completions.add("1000");
            completions.add("10000");
        }
        
        return completions;
    }
}