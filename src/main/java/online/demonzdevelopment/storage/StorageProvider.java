package online.demonzdevelopment.storage;

import online.demonzdevelopment.data.PlayerData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {
    void initialize();
    
    CompletableFuture<PlayerData> loadPlayerData(UUID uuid);
    
    CompletableFuture<Void> savePlayerData(PlayerData data);
    
    CompletableFuture<Boolean> playerExists(UUID uuid);
    
    CompletableFuture<UUID> getUUIDByName(String name);
    
    void close();
}