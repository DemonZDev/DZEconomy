package online.demonzdevelopment.dzeconomy.manager;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages combat tagging for players in PVP/PvE situations
 * Prevents request GUIs from opening when players are in combat
 * 
 * @author DemonZ Development
 * @version 1.2.0
 */
public class CombatTagManager {
    
    private final DZEconomy plugin;
    private final Map<UUID, Long> combatTags;
    private final Set<EntityType> dangerousMobs;
    
    public CombatTagManager(DZEconomy plugin) {
        this.plugin = plugin;
        this.combatTags = new ConcurrentHashMap<>();
        this.dangerousMobs = new HashSet<>();
        
        // Load dangerous mobs from config
        loadDangerousMobs();
    }
    
    /**
     * Load dangerous mobs list from configuration
     */
    private void loadDangerousMobs() {
        dangerousMobs.clear();
        
        if (!plugin.getConfigManager().getConfig().contains("combat-tagging.dangerous-mobs")) {
            // Set default dangerous mobs if not configured
            setDefaultDangerousMobs();
            return;
        }
        
        for (String mobName : plugin.getConfigManager().getConfig().getStringList("combat-tagging.dangerous-mobs")) {
            try {
                EntityType type = EntityType.valueOf(mobName.toUpperCase());
                dangerousMobs.add(type);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid mob type in dangerous-mobs list: " + mobName);
            }
        }
        
        plugin.getLogger().info("Loaded " + dangerousMobs.size() + " dangerous mob types for combat tagging");
    }
    
    /**
     * Set default dangerous mobs
     */
    private void setDefaultDangerousMobs() {
        dangerousMobs.add(EntityType.WITHER);
        dangerousMobs.add(EntityType.ENDER_DRAGON);
        dangerousMobs.add(EntityType.WARDEN);
        dangerousMobs.add(EntityType.CREEPER);
        dangerousMobs.add(EntityType.PHANTOM);
        dangerousMobs.add(EntityType.RAVAGER);
        dangerousMobs.add(EntityType.PIGLIN_BRUTE);
        dangerousMobs.add(EntityType.EVOKER);
        dangerousMobs.add(EntityType.VEX);
        dangerousMobs.add(EntityType.BLAZE);
        dangerousMobs.add(EntityType.GHAST);
    }
    
    /**
     * Tag a player as in combat
     * 
     * @param playerUUID The player's UUID
     */
    public void tagPlayer(UUID playerUUID) {
        if (!isEnabled()) {
            return;
        }
        
        combatTags.put(playerUUID, System.currentTimeMillis());
    }
    
    /**
     * Check if a player is in combat
     * 
     * @param playerUUID The player's UUID
     * @return true if player is in combat, false otherwise
     */
    public boolean isInCombat(UUID playerUUID) {
        if (!isEnabled()) {
            return false;
        }
        
        Long tagTime = combatTags.get(playerUUID);
        if (tagTime == null) {
            return false;
        }
        
        int duration = getCombatDuration();
        long expirationTime = tagTime + (duration * 1000L);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime >= expirationTime) {
            // Combat tag expired
            combatTags.remove(playerUUID);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get remaining combat time in seconds
     * 
     * @param playerUUID The player's UUID
     * @return Remaining seconds, or 0 if not in combat
     */
    public long getRemainingCombatTime(UUID playerUUID) {
        if (!isInCombat(playerUUID)) {
            return 0;
        }
        
        Long tagTime = combatTags.get(playerUUID);
        if (tagTime == null) {
            return 0;
        }
        
        int duration = getCombatDuration();
        long expirationTime = tagTime + (duration * 1000L);
        long currentTime = System.currentTimeMillis();
        long remaining = expirationTime - currentTime;
        
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * Remove combat tag from a player
     * 
     * @param playerUUID The player's UUID
     */
    public void untagPlayer(UUID playerUUID) {
        combatTags.remove(playerUUID);
    }
    
    /**
     * Clear all combat tags
     */
    public void clearAllTags() {
        combatTags.clear();
    }
    
    /**
     * Clean up expired combat tags
     */
    public void cleanupExpiredTags() {
        long currentTime = System.currentTimeMillis();
        int duration = getCombatDuration();
        
        combatTags.entrySet().removeIf(entry -> {
            long expirationTime = entry.getValue() + (duration * 1000L);
            return currentTime >= expirationTime;
        });
    }
    
    /**
     * Check if an entity type is considered dangerous
     * 
     * @param entityType The entity type to check
     * @return true if dangerous, false otherwise
     */
    public boolean isDangerousMob(EntityType entityType) {
        return dangerousMobs.contains(entityType);
    }
    
    /**
     * Check if combat tagging is enabled
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("combat-tagging.enabled", true);
    }
    
    /**
     * Check if request GUI should be blocked during combat
     * 
     * @return true if should block, false otherwise
     */
    public boolean shouldBlockRequestGUI() {
        return plugin.getConfigManager().getConfig().getBoolean("combat-tagging.block-request-gui-in-combat", true);
    }
    
    /**
     * Get combat duration in seconds
     * 
     * @return Combat duration in seconds
     */
    public int getCombatDuration() {
        return plugin.getConfigManager().getConfig().getInt("combat-tagging.duration", 30);
    }
    
    /**
     * Reload configuration
     */
    public void reload() {
        loadDangerousMobs();
        plugin.getLogger().info("Combat tag manager reloaded");
    }
    
    /**
     * Get number of players currently in combat
     * 
     * @return Number of players in combat
     */
    public int getPlayersInCombat() {
        cleanupExpiredTags();
        return combatTags.size();
    }
}
