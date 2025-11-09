package online.demonzdevelopment.currency;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.cache.BalanceCache;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.data.CurrencyRequest;
import online.demonzdevelopment.util.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Manages all currency operations including balances, transfers, and requests
 * Optimized with smart caching and async operations
 * 
 * @author DemonZ Development
 * @version 1.1.1
 */
public class CurrencyManager {
    
    private final DZEconomy plugin;
    private final Map<UUID, PlayerData> playerDataCache;
    private final Map<UUID, List<CurrencyRequest>> pendingRequests;
    private final BalanceCache balanceCache;
    
    public CurrencyManager(DZEconomy plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.balanceCache = new BalanceCache();
    }
    
    /**
     * Load player data from storage or create new
     */
    public PlayerData loadPlayerData(UUID uuid) {
        // Check cache first
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }
        
        // Load from storage
        PlayerData data = plugin.getStorageProvider().loadPlayerData(uuid);
        
        // If no data exists, create new
        if (data == null) {
            data = createNewPlayerData(uuid);
        }
        
        // Cache and return
        playerDataCache.put(uuid, data);
        return data;
    }
    
    /**
     * Create new player data with starting balances
     */
    private PlayerData createNewPlayerData(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        
        // Set starting balances from config
        data.setBalance(CurrencyType.MONEY, 
            plugin.getConfigManager().getConfig().getDouble("currencies.money.starting-balance", 50000.0));
        data.setBalance(CurrencyType.MOBCOIN, 
            plugin.getConfigManager().getConfig().getDouble("currencies.mobcoin.starting-balance", 500.0));
        data.setBalance(CurrencyType.GEM, 
            plugin.getConfigManager().getConfig().getDouble("currencies.gem.starting-balance", 5.0));
        
        data.setFirstJoin(System.currentTimeMillis());
        data.setLastSeen(System.currentTimeMillis());
        
        return data;
    }
    
    /**
     * Save player data to storage
     */
    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            plugin.getStorageProvider().savePlayerData(data);
        }
    }
    
    /**
     * Save player data asynchronously using CompletableFuture
     */
    public void savePlayerDataAsync(UUID uuid) {
        CompletableFuture.runAsync(() -> savePlayerData(uuid));
    }
    
    /**
     * Save all cached player data
     */
    public void saveAllPlayers() {
        for (UUID uuid : playerDataCache.keySet()) {
            savePlayerData(uuid);
        }
    }
    
    /**
     * Unload player data from cache (with cache invalidation)
     */
    public void unloadPlayerData(UUID uuid) {
        savePlayerData(uuid);
        playerDataCache.remove(uuid);
        balanceCache.invalidateAll(uuid);
    }
    
    /**
     * Get player's balance (with caching)
     */
    public double getBalance(UUID uuid, CurrencyType currency) {
        // Check cache first
        Double cached = balanceCache.getBalance(uuid, currency);
        if (cached != null) {
            return cached;
        }
        
        // Load from data
        PlayerData data = loadPlayerData(uuid);
        double balance = data.getBalance(currency);
        
        // Cache result
        balanceCache.cacheBalance(uuid, currency, balance);
        
        return balance;
    }
    
    /**
     * Set player's balance (with cache invalidation)
     */
    public void setBalance(UUID uuid, CurrencyType currency, double amount) {
        PlayerData data = loadPlayerData(uuid);
        data.setBalance(currency, NumberFormatter.truncateDecimal(amount));
        balanceCache.invalidate(uuid, currency);
        savePlayerDataAsync(uuid);
    }
    
    /**
     * Add to player's balance (with cache invalidation)
     */
    public void addBalance(UUID uuid, CurrencyType currency, double amount) {
        PlayerData data = loadPlayerData(uuid);
        double newBalance = data.getBalance(currency) + amount;
        data.setBalance(currency, NumberFormatter.truncateDecimal(newBalance));
        balanceCache.invalidate(uuid, currency);
        savePlayerDataAsync(uuid);
    }
    
    /**
     * Remove from player's balance (with cache invalidation)
     */
    public void removeBalance(UUID uuid, CurrencyType currency, double amount) {
        PlayerData data = loadPlayerData(uuid);
        double newBalance = data.getBalance(currency) - amount;
        data.setBalance(currency, NumberFormatter.truncateDecimal(newBalance));
        balanceCache.invalidate(uuid, currency);
        savePlayerDataAsync(uuid);
    }
    
    /**
     * Check if player has enough balance
     */
    public boolean hasBalance(UUID uuid, CurrencyType currency, double amount) {
        return getBalance(uuid, currency) >= amount;
    }
    
    /**
     * Get formatted balance with symbol
     */
    public String getFormattedBalance(UUID uuid, CurrencyType currency) {
        double balance = getBalance(uuid, currency);
        String symbol = getCurrencySymbol(currency);
        return symbol + NumberFormatter.formatShort(balance);
    }
    
    /**
     * Get currency symbol from config
     */
    public String getCurrencySymbol(CurrencyType currency) {
        String configPath = "currencies." + currency.getId() + ".symbol";
        return plugin.getConfigManager().getConfig().getString(configPath, currency.getDefaultSymbol());
    }
    
    /**
     * Get currency display name from config
     */
    public String getCurrencyDisplayName(CurrencyType currency) {
        String configPath = "currencies." + currency.getId() + ".display-name";
        return plugin.getConfigManager().getConfig().getString(configPath, currency.getDisplayName());
    }
    
    // Request management
    
    /**
     * Add a currency request
     */
    public void addRequest(CurrencyRequest request) {
        pendingRequests.computeIfAbsent(request.getRequestedPlayerUUID(), k -> new ArrayList<>()).add(request);
    }
    
    /**
     * Get pending request for a player
     */
    public CurrencyRequest getPendingRequest(UUID uuid) {
        List<CurrencyRequest> requests = pendingRequests.get(uuid);
        if (requests != null && !requests.isEmpty()) {
            return requests.get(0);
        }
        return null;
    }
    
    /**
     * Check if player has pending request
     */
    public boolean hasPendingRequest(UUID uuid) {
        return getPendingRequest(uuid) != null;
    }
    
    /**
     * Check if player has pending request with specific player
     */
    public boolean hasPendingRequestWith(UUID player1, UUID player2) {
        List<CurrencyRequest> requests1 = pendingRequests.get(player1);
        if (requests1 != null) {
            for (CurrencyRequest req : requests1) {
                if (req.getRequesterUUID().equals(player2)) {
                    return true;
                }
            }
        }
        
        List<CurrencyRequest> requests2 = pendingRequests.get(player2);
        if (requests2 != null) {
            for (CurrencyRequest req : requests2) {
                if (req.getRequesterUUID().equals(player1)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Remove a request
     */
    public void removeRequest(UUID uuid) {
        List<CurrencyRequest> requests = pendingRequests.get(uuid);
        if (requests != null && !requests.isEmpty()) {
            requests.remove(0);
            if (requests.isEmpty()) {
                pendingRequests.remove(uuid);
            }
        }
    }
    
    /**
     * Get all pending requests (for timeout checking)
     */
    public Map<UUID, List<CurrencyRequest>> getAllPendingRequests() {
        return pendingRequests;
    }
    
    /**
     * Get player data from cache
     */
    public PlayerData getPlayerData(UUID uuid) {
        return loadPlayerData(uuid);
    }
    
    /**
     * Get all cached player data
     */
    public Collection<PlayerData> getAllPlayerData() {
        return playerDataCache.values();
    }
}