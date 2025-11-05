package online.demonzdevelopment.dzeconomy;

import online.demonzdevelopment.dzeconomy.api.DZEconomyAPI;
import online.demonzdevelopment.dzeconomy.api.DZEconomyAPIImpl;
import online.demonzdevelopment.dzeconomy.command.EconomyCommand;
import online.demonzdevelopment.dzeconomy.command.GemCommand;
import online.demonzdevelopment.dzeconomy.command.MobCoinCommand;
import online.demonzdevelopment.dzeconomy.command.MoneyCommand;
import online.demonzdevelopment.dzeconomy.config.ConfigManager;
import online.demonzdevelopment.dzeconomy.manager.CurrencyManager;
import online.demonzdevelopment.dzeconomy.listener.EntityDeathListener;
import online.demonzdevelopment.dzeconomy.listener.PlayerDeathListener;
import online.demonzdevelopment.dzeconomy.listener.PlayerJoinListener;
import online.demonzdevelopment.dzeconomy.listener.PlayerQuitListener;
import online.demonzdevelopment.dzeconomy.gui.RequestGUIManager;
import online.demonzdevelopment.dzeconomy.integration.LuckPermsIntegration;
import online.demonzdevelopment.dzeconomy.integration.PlaceholderAPI;
import online.demonzdevelopment.dzeconomy.manager.RankManager;
import online.demonzdevelopment.dzeconomy.storage.StorageProvider;
import online.demonzdevelopment.dzeconomy.storage.impl.FlatFileStorageProvider;
import online.demonzdevelopment.dzeconomy.storage.impl.MySQLStorageProvider;
import online.demonzdevelopment.dzeconomy.storage.impl.SQLiteStorageProvider;
import online.demonzdevelopment.dzeconomy.task.AutoSaveTask;
import online.demonzdevelopment.dzeconomy.task.DailyResetTask;
import online.demonzdevelopment.dzeconomy.task.RequestTimeoutTask;
import online.demonzdevelopment.dzeconomy.update.UpdateChecker;
import online.demonzdevelopment.dzeconomy.update.UpdateManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DZEconomy - Professional Multi-Currency Economy Plugin
 * 
 * Main plugin class handling initialization, dependency management,
 * and lifecycle management for the economy system.
 * 
 * @author DemonZ Development
 * @version 1.2.0
 */
public final class DZEconomy extends JavaPlugin {

    private static DZEconomy instance;
    
    // Core managers
    private ConfigManager configManager;
    private StorageProvider storageProvider;
    private CurrencyManager currencyManager;
    private RankManager rankManager;
    private RequestGUIManager requestGUIManager;
    
    // Integration
    private LuckPermsIntegration luckPermsIntegration;
    
    // API
    private DZEconomyAPIImpl api;
    
    // Update checker
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;
        
        long startTime = System.currentTimeMillis();
        getLogger().info("==================================");
        getLogger().info("   DZEconomy v" + getDescription().getVersion());
        getLogger().info("   By DemonZ Development");
        getLogger().info("==================================");
        
        // Initialize configuration
        if (!initializeConfiguration()) {
            getLogger().severe("Failed to initialize configuration! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize storage
        if (!initializeStorage()) {
            getLogger().severe("Failed to initialize storage! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize integrations
        initializeIntegrations();
        
        // Initialize managers
        this.currencyManager = new CurrencyManager(this);
        this.rankManager = new RankManager(this);
        this.requestGUIManager = new RequestGUIManager(this);
        
        // Register commands
        registerCommands();
        
        // Register events
        registerEvents();
        
        // Register API
        registerAPI();
        
        // Start tasks
        startTasks();
        
        // Check for updates
        checkForUpdates();
        
        long endTime = System.currentTimeMillis();
        getLogger().info("==================================");
        getLogger().info("   DZEconomy enabled successfully!");
        getLogger().info("   Took " + (endTime - startTime) + "ms");
        getLogger().info("==================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down DZEconomy...");
        
        // Cancel all tasks
        getServer().getScheduler().cancelTasks(this);
        
        // Save all data
        if (currencyManager != null) {
            getLogger().info("Saving all player data...");
            currencyManager.saveAllPlayers();
        }
        
        // Close storage
        if (storageProvider != null) {
            getLogger().info("Closing storage connection...");
            storageProvider.close();
        }
        
        // Close GUI manager
        if (requestGUIManager != null) {
            requestGUIManager.closeAllGUIs();
        }
        
        getLogger().info("DZEconomy disabled successfully!");
    }
    
    /**
     * Initialize configuration files
     */
    private boolean initializeConfiguration() {
        try {
            getLogger().info("Loading configuration files...");
            this.configManager = new ConfigManager(this);
            configManager.loadAll();
            getLogger().info("Configuration loaded successfully!");
            return true;
        } catch (Exception e) {
            getLogger().severe("Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Initialize storage provider based on configuration
     */
    private boolean initializeStorage() {
        try {
            String storageType = configManager.getConfig().getString("storage.type", "FLATFILE").toUpperCase();
            getLogger().info("Initializing " + storageType + " storage...");
            
            switch (storageType) {
                case "MYSQL":
                    this.storageProvider = new MySQLStorageProvider(this);
                    break;
                case "SQLITE":
                    this.storageProvider = new SQLiteStorageProvider(this);
                    break;
                case "FLATFILE":
                default:
                    this.storageProvider = new FlatFileStorageProvider(this);
                    break;
            }
            
            storageProvider.initialize();
            getLogger().info("Storage initialized successfully!");
            return true;
        } catch (Exception e) {
            getLogger().severe("Failed to initialize storage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Initialize integrations with other plugins
     */
    private void initializeIntegrations() {
        // LuckPerms integration
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            getLogger().info("Hooking into LuckPerms...");
            this.luckPermsIntegration = new LuckPermsIntegration(this);
            getLogger().info("LuckPerms integration enabled!");
        } else {
            getLogger().warning("LuckPerms not found! Rank system will use default rank only.");
        }
        
        // PlaceholderAPI integration
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("Hooking into PlaceholderAPI...");
            new PlaceholderAPI(this).register();
            getLogger().info("PlaceholderAPI integration enabled!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not be available.");
        }
    }
    
    /**
     * Register all plugin commands
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("mobcoin").setExecutor(new MobCoinCommand(this));
        getCommand("gem").setExecutor(new GemCommand(this));
        getCommand("economy").setExecutor(new EconomyCommand(this));
        getLogger().info("Commands registered!");
    }
    
    /**
     * Register all event listeners
     */
    private void registerEvents() {
        getLogger().info("Registering event listeners...");
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getLogger().info("Event listeners registered!");
    }
    
    /**
     * Register the public API
     */
    private void registerAPI() {
        getLogger().info("Registering API...");
        this.api = new DZEconomyAPIImpl(this);
        getServer().getServicesManager().register(
            DZEconomyAPI.class,
            api,
            this,
            ServicePriority.Normal
        );
        getLogger().info("API registered successfully!");
    }
    
    /**
     * Start scheduled tasks
     */
    private void startTasks() {
        getLogger().info("Starting scheduled tasks...");
        
        // Auto-save task
        int autoSaveInterval = configManager.getConfig().getInt("storage.auto-save-interval", 5);
        new AutoSaveTask(this).runTaskTimerAsynchronously(this, 20L * 60 * autoSaveInterval, 20L * 60 * autoSaveInterval);
        
        // Daily reset task
        new DailyResetTask(this).runTaskTimer(this, 20L, 20L * 60); // Check every minute
        
        // Request timeout task
        new RequestTimeoutTask(this).runTaskTimer(this, 20L, 20L); // Check every second
        
        getLogger().info("Scheduled tasks started!");
    }
    
    /**
     * Check for plugin updates
     */
    private void checkForUpdates() {
        if (!configManager.getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }
        
        getLogger().info("Checking for updates...");
        
        updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates().thenAccept(hasUpdate -> {
            // Log status to console
            updateChecker.logUpdateStatus();
        });
        
        // Auto-update on start if enabled
        if (configManager.getConfig().getBoolean("updater.autoOnStart", false)) {
            getLogger().info("Auto-update on start is enabled. Checking for updates...");
            UpdateManager updateManager = new UpdateManager(this);
            updateManager.autoUpdate().thenAccept(result -> {
                if (result.isSuccess()) {
                    getLogger().info("Auto-update completed! Restart server to apply v" + result.getVersion());
                }
            });
        }
    }
    
    /**
     * Reload all configuration files
     */
    public void reload() {
        getLogger().info("Reloading DZEconomy...");
        
        // Reload configurations
        configManager.loadAll();
        
        // Reload rank cache for all online players
        rankManager.reloadAllPlayerRanks();
        
        // Check for updates again
        if (updateChecker != null) {
            updateChecker.checkForUpdates();
        }
        
        getLogger().info("DZEconomy reloaded successfully!");
    }
    
    // Getters
    
    public static DZEconomy getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public StorageProvider getStorageProvider() {
        return storageProvider;
    }
    
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }
    
    public RankManager getRankManager() {
        return rankManager;
    }
    
    public RequestGUIManager getRequestGUIManager() {
        return requestGUIManager;
    }
    
    public LuckPermsIntegration getLuckPermsIntegration() {
        return luckPermsIntegration;
    }
    
    public DZEconomyAPI getAPI() {
        return api;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}