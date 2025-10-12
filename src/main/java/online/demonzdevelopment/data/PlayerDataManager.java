package online.demonzdevelopment.data;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final DZEconomy plugin;
    private final StorageProvider storageProvider;
    private final Map<UUID, PlayerData> cache;

    public PlayerDataManager(DZEconomy plugin, StorageProvider storageProvider) {
        this.plugin = plugin;
        this.storageProvider = storageProvider;
        this.cache = new ConcurrentHashMap<>();
    }

    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid, String name) {
        if (cache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(cache.get(uuid));
        }
        
        return storageProvider.loadPlayerData(uuid).thenApply(data -> {
            if (data == null) {
                data = new PlayerData(uuid, name);
                initializeNewPlayer(data);
            } else {
                data.setName(name);
            }
            cache.put(uuid, data);
            return data;
        });
    }

    private void initializeNewPlayer(PlayerData data) {
        if (plugin.getConfigManager().isCurrencyEnabled(CurrencyType.MONEY)) {
            BigDecimal bonus = plugin.getConfigManager().getNewPlayerBonus(CurrencyType.MONEY);
            data.setBalance(CurrencyType.MONEY, bonus);
        }
        
        if (plugin.getConfigManager().isCurrencyEnabled(CurrencyType.MOBCOIN)) {
            BigDecimal bonus = plugin.getConfigManager().getNewPlayerBonus(CurrencyType.MOBCOIN);
            data.setBalance(CurrencyType.MOBCOIN, bonus);
        }
        
        if (plugin.getConfigManager().isCurrencyEnabled(CurrencyType.GEM)) {
            BigDecimal bonus = plugin.getConfigManager().getNewPlayerBonus(CurrencyType.GEM);
            data.setBalance(CurrencyType.GEM, bonus);
        }
        
        data.setDirty(true);
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    public Collection<PlayerData> getAllPlayerData() {
        return cache.values();
    }

    public CompletableFuture<Void> savePlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null || !data.isDirty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        return storageProvider.savePlayerData(data).thenRun(() -> {
            data.setDirty(false);
        });
    }

    public CompletableFuture<Void> saveAll() {
        CompletableFuture<?>[] futures = cache.values().stream()
            .filter(PlayerData::isDirty)
            .map(data -> storageProvider.savePlayerData(data).thenRun(() -> data.setDirty(false)))
            .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(futures);
    }

    public void saveAllSync() {
        for (PlayerData data : cache.values()) {
            if (data.isDirty()) {
                storageProvider.savePlayerData(data).join();
                data.setDirty(false);
            }
        }
    }

    public void unloadPlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null && data.isDirty()) {
            savePlayerData(uuid).join();
        }
        cache.remove(uuid);
    }

    public CompletableFuture<UUID> getUUIDByName(String name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            return CompletableFuture.completedFuture(player.getUniqueId());
        }
        
        for (PlayerData data : cache.values()) {
            if (data.getName().equalsIgnoreCase(name)) {
                return CompletableFuture.completedFuture(data.getUUID());
            }
        }
        
        return storageProvider.getUUIDByName(name);
    }
}