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
            case "migrate":
                handleMigrate(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "status":
                handleStatus(sender);
                break;
            case "info":
                handleInfo(sender);
                break;
            case "backup":
                handleBackup(sender);
                break;
            case "enable":
                handleEnable(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "disable":
                handleDisable(sender, Arrays.copyOfRange(args, 1, args.length));
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
     * Handle migrate command
     */
    private void handleMigrate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dzeconomy.admin") && !sender.isOp()) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /dzeconomy migrate <from> <to>"));
            sender.sendMessage(ColorUtil.translate("&7Example: &e/dzeconomy migrate flatfile mysql"));
            sender.sendMessage(ColorUtil.translate("&7Storage types: &eflatfile&7, &esqlite&7, &emysql"));
            return;
        }
        
        String fromType = args[0];
        String toType = args[1];
        
        sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &eStarting migration process..."));
        sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &7This may take a few minutes..."));
        
        // Execute async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            online.demonzdevelopment.dzeconomy.manager.MigrationManager migrationManager = 
                    new online.demonzdevelopment.dzeconomy.manager.MigrationManager(plugin);
            
            migrationManager.migrateData(sender, fromType, toType).thenAccept(result -> {
                if (result.isSuccess()) {
                    plugin.getLogger().info("Migration successful: " + result.getMigratedCount() + " players migrated");
                } else {
                    plugin.getLogger().warning("Migration failed: " + result.getMessage());
                }
            });
        });
    }
    
    /**
     * Handle status command
     */
    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("dzeconomy.admin") && !sender.isOp()) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        sender.sendMessage(ColorUtil.translate("&8&m                                            "));
        sender.sendMessage(ColorUtil.translate("&6&lDZEconomy Status"));
        sender.sendMessage(ColorUtil.translate("&8&m                                            "));
        sender.sendMessage(ColorUtil.translate("&7Version: &av" + plugin.getDescription().getVersion()));
        sender.sendMessage(ColorUtil.translate("&7Author: &eDemonZ Development"));
        
        // Storage info
        String storageType = plugin.getConfigManager().getConfig().getString("storage.type", "FLATFILE");
        sender.sendMessage(ColorUtil.translate("&7Storage: &e" + storageType));
        
        // Loaded players
        int loadedPlayers = plugin.getCurrencyManager().getAllLoadedPlayers().size();
        sender.sendMessage(ColorUtil.translate("&7Loaded Players: &a" + loadedPlayers));
        
        // Update checker status
        boolean updateCheckerEnabled = plugin.getConfigManager().getConfig().getBoolean("update-checker.enabled", true);
        sender.sendMessage(ColorUtil.translate("&7Update Checker: " + (updateCheckerEnabled ? "&aEnabled" : "&cDisabled")));
        
        // Auto-update status
        boolean autoUpdateEnabled = plugin.getConfigManager().getConfig().getBoolean("updater.enabled", true);
        sender.sendMessage(ColorUtil.translate("&7Auto-Update: " + (autoUpdateEnabled ? "&aEnabled" : "&cDisabled")));
        
        // Runtime update check status
        boolean runtimeCheckEnabled = plugin.getConfigManager().getConfig().getBoolean("update-checker.runtime-check-enabled", true);
        int runtimeInterval = plugin.getConfigManager().getConfig().getInt("update-checker.runtime-check-interval", 1);
        sender.sendMessage(ColorUtil.translate("&7Runtime Checks: " + (runtimeCheckEnabled ? "&aEnabled &7(" + runtimeInterval + "h)" : "&cDisabled")));
        
        // Check for updates
        if (plugin.getUpdateChecker() != null && plugin.getUpdateChecker().isUpdateAvailable()) {
            sender.sendMessage(ColorUtil.translate("&7Latest Version: &e&lv" + plugin.getUpdateChecker().getLatestVersion() + " &7(update available!)"));
        }
        
        sender.sendMessage(ColorUtil.translate("&8&m                                            "));
    }
    
    /**
     * Handle info command
     */
    private void handleInfo(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m                                            "));
        sender.sendMessage(ColorUtil.translate("&6&lDZEconomy - Professional Multi-Currency Plugin"));
        sender.sendMessage(ColorUtil.translate("&8&m                                            "));
        sender.sendMessage(ColorUtil.translate("&7Version: &av" + plugin.getDescription().getVersion()));
        sender.sendMessage(ColorUtil.translate("&7Platform: &ePaperMC 1.21.1"));
        sender.sendMessage(ColorUtil.translate("&7Java: &e21"));
        sender.sendMessage("");
        sender.sendMessage(ColorUtil.translate("&6Features:"));
        sender.sendMessage(ColorUtil.translate("&7• &eMulti-Currency System (Money, MobCoin, Gems)"));
        sender.sendMessage(ColorUtil.translate("&7• &eRank-Based Economy with LuckPerms"));
        sender.sendMessage(ColorUtil.translate("&7• &eRequest System with Interactive GUI"));
        sender.sendMessage(ColorUtil.translate("&7• &ePVP Economy & Mob Rewards"));
        sender.sendMessage(ColorUtil.translate("&7• &eStorage: FlatFile, SQLite, MySQL"));
        sender.sendMessage(ColorUtil.translate("&7• &ePlaceholderAPI Integration"));
        sender.sendMessage(ColorUtil.translate("&7• &eComprehensive Public API"));
        sender.sendMessage("");
        sender.sendMessage(ColorUtil.translate("&7Created by: &6DemonZ Development"));
        sender.sendMessage(ColorUtil.translate("&7Website: &bdemonzdevelopment.online"));
        sender.sendMessage(ColorUtil.translate("&8&m                                            "));
    }
    
    /**
     * Handle backup command
     */
    private void handleBackup(CommandSender sender) {
        if (!sender.hasPermission("dzeconomy.admin") && !sender.isOp()) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &eCreating backup..."));
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Save all player data first
                plugin.getCurrencyManager().saveAllPlayers();
                
                // Create backup
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String timestamp = sdf.format(new java.util.Date());
                java.io.File backupFolder = new java.io.File(plugin.getDataFolder(), "backups/" + timestamp);
                backupFolder.mkdirs();
                
                // Copy data folder
                java.io.File dataFolder = new java.io.File(plugin.getDataFolder(), "data");
                if (dataFolder.exists()) {
                    copyFolder(dataFolder.toPath(), backupFolder.toPath());
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &a&l✓ Backup created!"));
                    sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &7Location: &ebackups/" + timestamp));
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create backup: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &cFailed to create backup!"));
                });
            }
        });
    }
    
    /**
     * Copy folder recursively
     */
    private void copyFolder(java.nio.file.Path source, java.nio.file.Path destination) throws java.io.IOException {
        java.nio.file.Files.walk(source).forEach(sourcePath -> {
            try {
                java.nio.file.Path destPath = destination.resolve(source.relativize(sourcePath));
                if (java.nio.file.Files.isDirectory(sourcePath)) {
                    java.nio.file.Files.createDirectories(destPath);
                } else {
                    java.nio.file.Files.copy(sourcePath, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (java.io.IOException e) {
                plugin.getLogger().warning("Failed to copy: " + sourcePath);
            }
        });
    }
    
    /**
     * Handle enable command
     */
    private void handleEnable(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dzeconomy.admin") && !sender.isOp()) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /dzeconomy enable <feature>"));
            sender.sendMessage(ColorUtil.translate("&7Features: &eauto-update&7, &eruntime-checks"));
            return;
        }
        
        String feature = args[0].toLowerCase();
        
        if (feature.equals("auto-update")) {
            plugin.getConfigManager().getConfig().set("updater.enabled", true);
            plugin.getConfigManager().save(online.demonzdevelopment.dzeconomy.config.ConfigManager.ConfigType.CONFIG);
            sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &aAuto-update enabled!"));
        } else if (feature.equals("runtime-checks")) {
            plugin.getConfigManager().getConfig().set("update-checker.runtime-check-enabled", true);
            plugin.getConfigManager().save(online.demonzdevelopment.dzeconomy.config.ConfigManager.ConfigType.CONFIG);
            sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &aRuntime update checks enabled!"));
        } else {
            sender.sendMessage(ColorUtil.translate("&cUnknown feature: " + feature));
        }
    }
    
    /**
     * Handle disable command
     */
    private void handleDisable(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dzeconomy.admin") && !sender.isOp()) {
            sendMessage(sender, "general.no-permission", null);
            return;
        }
        
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /dzeconomy disable <feature>"));
            sender.sendMessage(ColorUtil.translate("&7Features: &eauto-update&7, &eruntime-checks"));
            return;
        }
        
        String feature = args[0].toLowerCase();
        
        if (feature.equals("auto-update")) {
            plugin.getConfigManager().getConfig().set("updater.enabled", false);
            plugin.getConfigManager().save(online.demonzdevelopment.dzeconomy.config.ConfigManager.ConfigType.CONFIG);
            sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &cAuto-update disabled!"));
        } else if (feature.equals("runtime-checks")) {
            plugin.getConfigManager().getConfig().set("update-checker.runtime-check-enabled", false);
            plugin.getConfigManager().save(online.demonzdevelopment.dzeconomy.config.ConfigManager.ConfigType.CONFIG);
            sender.sendMessage(ColorUtil.translate("&8[&6DZ&eEconomy&8] &cRuntime update checks disabled!"));
        } else {
            sender.sendMessage(ColorUtil.translate("&cUnknown feature: " + feature));
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
            completions.add("info");
            if (sender.hasPermission("dzeconomy.admin") || sender.isOp()) {
                completions.add("reload");
                completions.add("credits");
                completions.add("update");
                completions.add("migrate");
                completions.add("status");
                completions.add("backup");
                completions.add("enable");
                completions.add("disable");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            if (sender.hasPermission("dzeconomy.admin") || sender.isOp()) {
                completions.add("flatfile");
                completions.add("sqlite");
                completions.add("mysql");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("migrate")) {
            if (sender.hasPermission("dzeconomy.admin") || sender.isOp()) {
                completions.add("flatfile");
                completions.add("sqlite");
                completions.add("mysql");
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable"))) {
            if (sender.hasPermission("dzeconomy.admin") || sender.isOp()) {
                completions.add("auto-update");
                completions.add("runtime-checks");
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