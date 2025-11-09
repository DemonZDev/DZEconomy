package online.demonzdevelopment.storage.impl;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.storage.StorageProvider;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * FlatFile (YAML) storage implementation
 */
public class FlatFileStorageProvider implements StorageProvider {
    
    private final DZEconomy plugin;
    private File dataFolder;
    
    public FlatFileStorageProvider(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void initialize() {
        dataFolder = new File(plugin.getDataFolder(), "data/players");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        
        if (!playerFile.exists()) {
            return null;
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerFile);
        PlayerData data = new PlayerData(uuid);
        
        // Load basic info
        data.setUsername(yaml.getString("username", "Unknown"));
        data.setFirstJoin(yaml.getLong("first-join", System.currentTimeMillis()));
        data.setLastSeen(yaml.getLong("last-seen", System.currentTimeMillis()));
        
        // Load balances
        for (CurrencyType type : CurrencyType.values()) {
            String path = "balances." + type.getId();
            data.setBalance(type, yaml.getDouble(path, 0.0));
        }
        
        // Load statistics
        for (CurrencyType type : CurrencyType.values()) {
            data.getMoneySent().put(type, yaml.getLong("statistics." + type.getId() + "-sent", 0L));
            data.getMoneyReceived().put(type, yaml.getLong("statistics." + type.getId() + "-received", 0L));
        }
        
        // Load daily limits
        for (CurrencyType type : CurrencyType.values()) {
            data.getDailySendCounts().put(type, yaml.getInt("daily-limits." + type.getId() + "-sends-today", 0));
            data.getDailyRequestCounts().put(type, yaml.getInt("daily-limits." + type.getId() + "-requests-today", 0));
        }
        data.setLastDailyReset(yaml.getLong("daily-limits.last-reset", 0L));
        
        // Load cooldowns
        for (CurrencyType type : CurrencyType.values()) {
            data.getSendCooldowns().put(type, yaml.getLong("cooldowns." + type.getId() + "-send", 0L));
            data.getRequestCooldowns().put(type, yaml.getLong("cooldowns." + type.getId() + "-request", 0L));
        }
        
        return data;
    }
    
    @Override
    public void savePlayerData(PlayerData playerData) {
        File playerFile = new File(dataFolder, playerData.getUUID().toString() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        
        // Save basic info
        yaml.set("uuid", playerData.getUUID().toString());
        yaml.set("username", playerData.getUsername());
        yaml.set("first-join", playerData.getFirstJoin());
        yaml.set("last-seen", playerData.getLastSeen());
        
        // Save balances
        for (CurrencyType type : CurrencyType.values()) {
            yaml.set("balances." + type.getId(), playerData.getBalance(type));
        }
        
        // Save statistics
        for (CurrencyType type : CurrencyType.values()) {
            yaml.set("statistics." + type.getId() + "-sent", playerData.getMoneySent().get(type));
            yaml.set("statistics." + type.getId() + "-received", playerData.getMoneyReceived().get(type));
        }
        
        // Save daily limits
        for (CurrencyType type : CurrencyType.values()) {
            yaml.set("daily-limits." + type.getId() + "-sends-today", playerData.getDailySendCounts().get(type));
            yaml.set("daily-limits." + type.getId() + "-requests-today", playerData.getDailyRequestCounts().get(type));
        }
        yaml.set("daily-limits.last-reset", playerData.getLastDailyReset());
        
        // Save cooldowns
        for (CurrencyType type : CurrencyType.values()) {
            yaml.set("cooldowns." + type.getId() + "-send", playerData.getSendCooldowns().get(type));
            yaml.set("cooldowns." + type.getId() + "-request", playerData.getRequestCooldowns().get(type));
        }
        
        try {
            yaml.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + playerData.getUUID() + ": " + e.getMessage());
        }
    }
    
    @Override
    public boolean playerDataExists(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        return playerFile.exists();
    }
    
    @Override
    public void deletePlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        if (playerFile.exists()) {
            playerFile.delete();
        }
    }
    
    @Override
    public void close() {
        // No connections to close for flatfile
    }
}