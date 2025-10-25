package online.demonzdevelopment.cache;

import online.demonzdevelopment.currency.CurrencyType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Smart caching system for player balances with TTL and invalidation
 * 
 * @author DemonZ Development
 * @version 1.1.1
 */
public class BalanceCache {
    
    private static final long CACHE_TTL_MS = 5000L; // 5 seconds
    private final Map<String, CachedBalance> cache = new ConcurrentHashMap<>();
    private final Map<String, CachedFormattedMessage> messageCache = new ConcurrentHashMap<>();
    
    /**
     * Get cached balance or return null if expired/missing
     */
    public Double getBalance(UUID player, CurrencyType currency) {
        String key = generateKey(player, currency);
        CachedBalance cached = cache.get(key);
        
        if (cached != null && !cached.isExpired()) {
            return cached.balance;
        }
        
        cache.remove(key);
        return null;
    }
    
    /**
     * Cache a balance value
     */
    public void cacheBalance(UUID player, CurrencyType currency, double balance) {
        String key = generateKey(player, currency);
        cache.put(key, new CachedBalance(balance, System.currentTimeMillis()));
    }
    
    /**
     * Invalidate cache for specific player and currency
     */
    public void invalidate(UUID player, CurrencyType currency) {
        String key = generateKey(player, currency);
        cache.remove(key);
    }
    
    /**
     * Invalidate all caches for a player
     */
    public void invalidateAll(UUID player) {
        for (CurrencyType currency : CurrencyType.values()) {
            invalidate(player, currency);
        }
    }
    
    /**
     * Clear entire cache
     */
    public void clearAll() {
        cache.clear();
        messageCache.clear();
    }
    
    /**
     * Get cached formatted message
     */
    public String getCachedMessage(String key) {
        CachedFormattedMessage cached = messageCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.message;
        }
        messageCache.remove(key);
        return null;
    }
    
    /**
     * Cache a formatted message
     */
    public void cacheMessage(String key, String message) {
        messageCache.put(key, new CachedFormattedMessage(message, System.currentTimeMillis()));
    }
    
    /**
     * Generate cache key
     */
    private String generateKey(UUID player, CurrencyType currency) {
        return player.toString() + ":" + currency.getId();
    }
    
    /**
     * Cached balance holder
     */
    private static class CachedBalance {
        final double balance;
        final long timestamp;
        
        CachedBalance(double balance, long timestamp) {
            this.balance = balance;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    /**
     * Cached formatted message holder
     */
    private static class CachedFormattedMessage {
        final String message;
        final long timestamp;
        
        CachedFormattedMessage(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
