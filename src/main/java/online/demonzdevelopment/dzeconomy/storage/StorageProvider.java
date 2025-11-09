package online.demonzdevelopment.dzeconomy.storage;

import online.demonzdevelopment.dzeconomy.data.PlayerData;

import java.util.UUID;

/**
 * Abstract storage provider interface for different storage types
 */
public interface StorageProvider {
    
    /**
     * Initialize the storage system
     */
    void initialize();
    
    /**
     * Load player data from storage
     * @return PlayerData object or null if not found
     */
    PlayerData loadPlayerData(UUID uuid);
    
    /**
     * Save player data to storage
     */
    void savePlayerData(PlayerData playerData);
    
    /**
     * Check if player data exists
     */
    boolean playerDataExists(UUID uuid);
    
    /**
     * Delete player data
     */
    void deletePlayerData(UUID uuid);
    
    /**
     * Close storage connections
     */
    void close();
}