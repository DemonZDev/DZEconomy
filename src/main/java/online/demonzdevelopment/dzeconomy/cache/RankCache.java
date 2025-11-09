package online.demonzdevelopment.dzeconomy.cache;

import online.demonzdevelopment.dzeconomy.rank.Rank;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caching system for rank data with efficient invalidation
 * 
 * @author DemonZ Development
 * @version 1.1.1
 */
public class RankCache {
    
    private static final long CACHE_TTL_MS = 300000L; // 5 minutes
    private final Map<UUID, CachedRank> cache = new ConcurrentHashMap<>();
    
    /**
     * Get cached rank or return null if expired/missing
     */
    public Rank getRank(UUID player) {
        CachedRank cached = cache.get(player);
        
        if (cached != null && !cached.isExpired()) {
            return cached.rank;
        }
        
        cache.remove(player);
        return null;
    }
    
    /**
     * Cache a rank
     */
    public void cacheRank(UUID player, Rank rank) {
        cache.put(player, new CachedRank(rank, System.currentTimeMillis()));
    }
    
    /**
     * Invalidate cache for specific player
     */
    public void invalidate(UUID player) {
        cache.remove(player);
    }
    
    /**
     * Clear entire cache
     */
    public void clearAll() {
        cache.clear();
    }
    
    /**
     * Cached rank holder
     */
    private static class CachedRank {
        final Rank rank;
        final long timestamp;
        
        CachedRank(Rank rank, long timestamp) {
            this.rank = rank;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
