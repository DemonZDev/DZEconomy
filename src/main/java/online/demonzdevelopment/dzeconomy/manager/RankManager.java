package online.demonzdevelopment.dzeconomy.manager;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.cache.RankCache;
import online.demonzdevelopment.dzeconomy.rank.Rank;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player ranks and rank data
 * Optimized with smart caching
 * 
 * @author DemonZ Development
 * @version 1.1.1
 */
public class RankManager {
    
    private final DZEconomy plugin;
    private final Map<String, Rank> ranks;
    private final RankCache rankCache;
    
    public RankManager(DZEconomy plugin) {
        this.plugin = plugin;
        this.ranks = new HashMap<>();
        this.rankCache = new RankCache();
        loadRanks();
    }
    
    /**
     * Load all ranks from ranks.yml
     */
    public void loadRanks() {
        ranks.clear();
        
        ConfigurationSection ranksSection = plugin.getConfigManager().getRanks().getConfigurationSection("ranks");
        if (ranksSection == null) {
            plugin.getLogger().warning("No ranks defined in ranks.yml!");
            return;
        }
        
        for (String rankId : ranksSection.getKeys(false)) {
            try {
                Rank rank = loadRank(rankId, ranksSection.getConfigurationSection(rankId));
                ranks.put(rankId.toLowerCase(), rank);
                plugin.getLogger().info("Loaded rank: " + rankId);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load rank " + rankId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("Loaded " + ranks.size() + " rank(s)");
    }
    
    /**
     * Load a single rank from configuration
     */
    private Rank loadRank(String id, ConfigurationSection section) {
        String displayName = section.getString("display-name", id);
        int priority = section.getInt("priority", 0);
        
        // Load money settings
        Rank.RankCurrencySettings moneySettings = loadCurrencySettings(section.getConfigurationSection("money"));
        
        // Load mobcoin settings
        Rank.RankCurrencySettings mobcoinSettings = loadCurrencySettings(section.getConfigurationSection("mobcoin"));
        
        // Load gem settings
        Rank.RankCurrencySettings gemSettings = loadCurrencySettings(section.getConfigurationSection("gem"));
        
        // Load conversion settings
        ConfigurationSection conversionSection = section.getConfigurationSection("conversion");
        boolean conversionEnabled = conversionSection != null && conversionSection.getBoolean("enabled", true);
        double conversionTax = conversionSection != null ? conversionSection.getDouble("tax", 3.0) : 3.0;
        
        return new Rank(id, displayName, priority, moneySettings, mobcoinSettings, gemSettings, 
                       conversionEnabled, conversionTax);
    }
    
    /**
     * Load currency settings from configuration section
     */
    private Rank.RankCurrencySettings loadCurrencySettings(ConfigurationSection section) {
        if (section == null) {
            // Return default settings
            return new Rank.RankCurrencySettings(true, 5.0, 300, 5, 5, 300, 0.0);
        }
        
        boolean enabled = section.getBoolean("enabled", true);
        double transferTax = section.getDouble("transfer-tax", 5.0);
        int transferCooldown = section.getInt("transfer-cooldown", 300);
        int dailyTransferLimit = section.getInt("daily-transfer-limit", 5);
        int dailyRequestLimit = section.getInt("daily-request-limit", 5);
        int requestCooldown = section.getInt("request-cooldown", 300);
        double bossKillBonus = section.getDouble("boss-kill-bonus", 0.0);
        
        return new Rank.RankCurrencySettings(enabled, transferTax, transferCooldown,
                dailyTransferLimit, dailyRequestLimit, requestCooldown, bossKillBonus);
    }
    
    /**
     * Get a player's rank (with smart caching)
     */
    public Rank getPlayerRank(UUID uuid) {
        // Check cache first
        Rank cached = rankCache.getRank(uuid);
        if (cached != null) {
            return cached;
        }
        
        // Load rank
        Rank rank = loadPlayerRank(uuid);
        rankCache.cacheRank(uuid, rank);
        return rank;
    }
    
    /**
     * Load a player's rank from LuckPerms
     */
    private Rank loadPlayerRank(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        
        // Check if LuckPerms integration is available
        if (plugin.getLuckPermsIntegration() != null && player != null) {
            String primaryGroup = plugin.getLuckPermsIntegration().getPrimaryGroup(player);
            
            if (primaryGroup != null) {
                Rank rank = ranks.get(primaryGroup.toLowerCase());
                if (rank != null) {
                    return rank;
                }
            }
        }
        
        // Fallback to default rank
        String fallbackRankId = plugin.getConfigManager().getConfig()
                .getString("integrations.luckperms.fallback-rank", "default");
        
        Rank fallbackRank = ranks.get(fallbackRankId.toLowerCase());
        if (fallbackRank == null) {
            // If even default doesn't exist, create a basic one
            plugin.getLogger().warning("Fallback rank '" + fallbackRankId + "' not found! Using basic default.");
            return createBasicDefaultRank();
        }
        
        return fallbackRank;
    }
    
    /**
     * Create a basic default rank if none exists
     */
    private Rank createBasicDefaultRank() {
        Rank.RankCurrencySettings defaultSettings = 
                new Rank.RankCurrencySettings(true, 5.0, 300, 5, 5, 300, 0.0);
        
        return new Rank("default", "&7Default", 0, defaultSettings, defaultSettings, 
                       defaultSettings, true, 3.0);
    }
    
    /**
     * Reload a specific player's rank
     */
    public void reloadPlayerRank(UUID uuid) {
        rankCache.invalidate(uuid);
        getPlayerRank(uuid);
    }
    
    /**
     * Reload all online players' ranks
     */
    public void reloadAllPlayerRanks() {
        rankCache.clearAll();
        for (Player player : Bukkit.getOnlinePlayers()) {
            getPlayerRank(player.getUniqueId());
        }
    }
    
    /**
     * Get all loaded ranks
     */
    public Collection<Rank> getAllRanks() {
        return ranks.values();
    }
    
    /**
     * Get a rank by ID
     */
    public Rank getRankById(String id) {
        return ranks.get(id.toLowerCase());
    }
}