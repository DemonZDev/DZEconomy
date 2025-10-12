package online.demonzdevelopment;

import online.demonzdevelopment.api.EconomyService;
import online.demonzdevelopment.api.EconomyServiceImpl;
import online.demonzdevelopment.command.*;
import online.demonzdevelopment.config.ConfigManager;
import online.demonzdevelopment.config.MessageManager;
import online.demonzdevelopment.config.MobRewardsConfig;
import online.demonzdevelopment.config.RankManager;
import online.demonzdevelopment.data.DailyResetService;
import online.demonzdevelopment.data.PlayerDataManager;
import online.demonzdevelopment.listener.MobKillListener;
import online.demonzdevelopment.listener.PlayerDeathListener;
import online.demonzdevelopment.listener.PlayerJoinListener;
import online.demonzdevelopment.manager.EconomyManager;
import online.demonzdevelopment.manager.RequestManager;
import online.demonzdevelopment.placeholder.DZEconomyExpansion;
import online.demonzdevelopment.storage.FlatFileStorageProvider;
import online.demonzdevelopment.storage.MySQLStorageProvider;
import online.demonzdevelopment.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class DZEconomy extends JavaPlugin {
    private ConfigManager configManager;
    private MessageManager messageManager;
    private RankManager rankManager;
    private MobRewardsConfig mobRewardsConfig;
    private StorageProvider storageProvider;
    private PlayerDataManager playerDataManager;
    private EconomyManager economyManager;
    private RequestManager requestManager;
    private DailyResetService dailyResetService;
    private EconomyService economyService;
    private boolean placeholderAPIHooked = false;
    private boolean luckPermsHooked = false;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        
        getLogger().info("Enabling DZEconomy v1.0.0");
        
        initializeConfigs();
        initializeStorage();
        initializeManagers();
        registerCommands();
        registerListeners();
        registerAPI();
        hookExternalPlugins();
        startTasks();
        
        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("DZEconomy v1.0.0 enabled successfully in " + loadTime + "ms");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling DZEconomy v1.0.0");
        
        if (requestManager != null) {
            requestManager.shutdown();
        }
        
        if (dailyResetService != null) {
            dailyResetService.shutdown();
        }
        
        if (playerDataManager != null) {
            playerDataManager.saveAllSync();
        }
        
        if (storageProvider != null) {
            storageProvider.close();
        }
        
        getLogger().info("DZEconomy v1.0.0 disabled successfully");
    }

    private void initializeConfigs() {
        saveDefaultConfig();
        
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        rankManager = new RankManager(this);
        mobRewardsConfig = new MobRewardsConfig(this);
        
        configManager.load();
        messageManager.load();
        rankManager.load();
        mobRewardsConfig.load();
    }

    private void initializeStorage() {
        if (configManager.isDatabaseEnabled()) {
            storageProvider = new MySQLStorageProvider(this, configManager);
            getLogger().info("Using MySQL storage provider");
        } else {
            storageProvider = new FlatFileStorageProvider(this);
            getLogger().info("Using flat-file storage provider");
        }
        
        storageProvider.initialize();
    }

    private void initializeManagers() {
        playerDataManager = new PlayerDataManager(this, storageProvider);
        economyManager = new EconomyManager(this, configManager, rankManager, playerDataManager);
        requestManager = new RequestManager(this, economyManager, messageManager);
        dailyResetService = new DailyResetService(this, playerDataManager);
    }

    private void registerCommands() {
        CommandRegistrar registrar = new CommandRegistrar(this);
        registrar.registerAll();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new MobKillListener(this), this);
    }

    private void registerAPI() {
        economyService = new EconomyServiceImpl(this);
        getServer().getServicesManager().register(
            EconomyService.class,
            economyService,
            this,
            ServicePriority.Normal
        );
        getLogger().info("DZEconomy API registered with ServicesManager");
    }

    private void hookExternalPlugins() {
        if (configManager.isPlaceholderAPIEnabled() && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DZEconomyExpansion(this).register();
            placeholderAPIHooked = true;
            getLogger().info("Hooked into PlaceholderAPI");
        } else {
            getLogger().warning("PlaceholderAPI not found - placeholders will not be available");
        }
        
        if (configManager.isLuckPermsEnabled() && Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsHooked = true;
            getLogger().info("Hooked into LuckPerms");
        } else {
            getLogger().warning("LuckPerms not found - using default rank for all players");
        }
    }

    private void startTasks() {
        int autoSaveInterval = configManager.getAutoSaveInterval() * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            playerDataManager.saveAll().thenRun(() -> {
                if (configManager.isDebugMode()) {
                    getLogger().info("Auto-saved all player data");
                }
            });
        }, autoSaveInterval, autoSaveInterval);
        
        dailyResetService.start();
    }

    public void reload() {
        configManager.load();
        messageManager.load();
        rankManager.load();
        mobRewardsConfig.load();
        reloadConfig();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public MobRewardsConfig getMobRewardsConfig() {
        return mobRewardsConfig;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public EconomyService getAPI() {
        return economyService;
    }

    public boolean isPlaceholderAPIHooked() {
        return placeholderAPIHooked;
    }

    public boolean isLuckPermsHooked() {
        return luckPermsHooked;
    }
}