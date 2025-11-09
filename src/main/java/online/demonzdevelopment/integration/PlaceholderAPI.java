package online.demonzdevelopment.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.rank.Rank;
import online.demonzdevelopment.util.NumberFormatter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlaceholderAPI expansion for DZEconomy
 * With smart caching for improved performance
 * 
 * @author DemonZ Development
 * @version 1.1.1
 */
public class PlaceholderAPI extends PlaceholderExpansion {
    
    private final DZEconomy plugin;
    private final Map<String, CachedValue> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 3000L; // 3 second cache (increased for better performance)
    
    public PlaceholderAPI(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    @NotNull
    public String getIdentifier() {
        return "dz";
    }
    
    @Override
    @NotNull
    public String getAuthor() {
        return "DemonZ Development";
    }
    
    @Override
    @NotNull
    public String getVersion() {
        return "1.1.1";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        // Use caching for frequently accessed placeholders
        String cacheKey = player.getUniqueId().toString() + ":" + params;
        
        // Money placeholders (with caching)
        if (params.equals("money")) {
            return getCached(cacheKey, () -> {
                double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), CurrencyType.MONEY);
                return NumberFormatter.formatShort(balance);
            });
        }
        
        if (params.equals("money_full")) {
            return getCached(cacheKey, () -> {
                double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), CurrencyType.MONEY);
                return NumberFormatter.formatFull(balance);
            });
        }
        
        if (params.equals("money_formatted")) {
            return getCached(cacheKey, () -> 
                plugin.getCurrencyManager().getFormattedBalance(player.getUniqueId(), CurrencyType.MONEY)
            );
        }
        
        // MobCoin placeholders (with caching)
        if (params.equals("mobcoin")) {
            return getCached(cacheKey, () -> {
                double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), CurrencyType.MOBCOIN);
                return NumberFormatter.formatShort(balance);
            });
        }
        
        if (params.equals("mobcoin_full")) {
            return getCached(cacheKey, () -> {
                double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), CurrencyType.MOBCOIN);
                return NumberFormatter.formatFull(balance);
            });
        }
        
        if (params.equals("mobcoin_formatted")) {
            return getCached(cacheKey, () -> 
                plugin.getCurrencyManager().getFormattedBalance(player.getUniqueId(), CurrencyType.MOBCOIN)
            );
        }
        
        // Gem placeholders (with caching)
        if (params.equals("gem")) {
            return getCached(cacheKey, () -> {
                double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), CurrencyType.GEM);
                return NumberFormatter.formatShort(balance);
            });
        }
        
        if (params.equals("gem_full")) {
            return getCached(cacheKey, () -> {
                double balance = plugin.getCurrencyManager().getBalance(player.getUniqueId(), CurrencyType.GEM);
                return NumberFormatter.formatFull(balance);
            });
        }
        
        if (params.equals("gem_formatted")) {
            return getCached(cacheKey, () -> 
                plugin.getCurrencyManager().getFormattedBalance(player.getUniqueId(), CurrencyType.GEM)
            );
        }
        
        // Rank placeholders (with caching)
        if (params.equals("rank")) {
            return getCached(cacheKey, () -> {
                Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
                return rank.getDisplayName();
            });
        }
        
        if (params.equals("rank_priority")) {
            return getCached(cacheKey, () -> {
                Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
                return String.valueOf(rank.getPriority());
            });
        }
        
        return null;
    }
    
    /**
     * Get cached value or compute new one
     */
    private String getCached(String key, java.util.function.Supplier<String> supplier) {
        CachedValue cached = cache.get(key);
        long now = System.currentTimeMillis();
        
        if (cached != null && (now - cached.timestamp) < CACHE_TTL) {
            return cached.value;
        }
        
        String value = supplier.get();
        cache.put(key, new CachedValue(value, now));
        return value;
    }
    
    /**
     * Cached value holder
     */
    private static class CachedValue {
        final String value;
        final long timestamp;
        
        CachedValue(String value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}