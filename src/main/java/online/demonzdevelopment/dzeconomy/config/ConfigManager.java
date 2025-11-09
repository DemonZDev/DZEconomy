package online.demonzdevelopment.dzeconomy.config;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Manages all plugin configuration files
 */
public class ConfigManager {
    
    private final DZEconomy plugin;
    
    private FileConfiguration config;
    private FileConfiguration ranks;
    private FileConfiguration mobRewards;
    private FileConfiguration messages;
    
    private File configFile;
    private File ranksFile;
    private File mobRewardsFile;
    private File messagesFile;
    
    public ConfigManager(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all configuration files
     */
    public void loadAll() {
        // Create plugin folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Load each config file
        loadConfig();
        loadRanks();
        loadMobRewards();
        loadMessages();
    }
    
    /**
     * Load main config.yml
     */
    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load defaults
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            config.setDefaults(defConfig);
        }
    }
    
    /**
     * Load ranks.yml
     */
    private void loadRanks() {
        ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        
        if (!ranksFile.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        
        ranks = YamlConfiguration.loadConfiguration(ranksFile);
    }
    
    /**
     * Load mob-rewards.yml
     */
    private void loadMobRewards() {
        mobRewardsFile = new File(plugin.getDataFolder(), "mob-rewards.yml");
        
        if (!mobRewardsFile.exists()) {
            plugin.saveResource("mob-rewards.yml", false);
        }
        
        mobRewards = YamlConfiguration.loadConfiguration(mobRewardsFile);
    }
    
    /**
     * Load messages.yml
     */
    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    /**
     * Save a configuration file
     */
    public void save(ConfigType type) {
        try {
            switch (type) {
                case CONFIG:
                    config.save(configFile);
                    break;
                case RANKS:
                    ranks.save(ranksFile);
                    break;
                case MOB_REWARDS:
                    mobRewards.save(mobRewardsFile);
                    break;
                case MESSAGES:
                    messages.save(messagesFile);
                    break;
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + type.name().toLowerCase() + ".yml: " + e.getMessage());
        }
    }
    
    /**
     * Reload all configuration files
     */
    public void reloadAll() {
        config = YamlConfiguration.loadConfiguration(configFile);
        ranks = YamlConfiguration.loadConfiguration(ranksFile);
        mobRewards = YamlConfiguration.loadConfiguration(mobRewardsFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    // Getters
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getRanks() {
        return ranks;
    }
    
    public FileConfiguration getMobRewards() {
        return mobRewards;
    }
    
    public FileConfiguration getMessages() {
        return messages;
    }
    
    /**
     * Configuration type enum
     */
    public enum ConfigType {
        CONFIG,
        RANKS,
        MOB_REWARDS,
        MESSAGES
    }
}