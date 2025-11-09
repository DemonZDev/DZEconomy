package online.demonzdevelopment.event;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.rank.Rank;
import online.demonzdevelopment.util.ColorUtil;
import online.demonzdevelopment.util.MessagesUtil;
import online.demonzdevelopment.util.NumberFormatter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;
import java.util.Map;

/**
 * Handles entity death events for mob kill rewards
 */
public class EntityDeathListener implements Listener {
    
    private final DZEconomy plugin;
    private final MessagesUtil messageUtil;
    
    public EntityDeathListener(DZEconomy plugin) {
        this.plugin = plugin;
        this.messageUtil = new MessagesUtil(plugin);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Early exit checks (performance optimization)
        if (!plugin.getConfigManager().getMobRewards().getBoolean("global.enabled", true)) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            if (plugin.getConfigManager().getMobRewards().getBoolean("global.require-player-kill", true)) {
                return;
            } else {
                return;
            }
        }
        
        EntityType entityType = event.getEntityType();
        
        // Check mob category and calculate reward
        double reward = getMobReward(entityType);
        
        if (reward <= 0) {
            return;
        }
        
        // Check if boss mob
        boolean isBoss = isBossMob(entityType);
        double finalReward = reward;
        double bonusPercent = 0.0;
        
        if (isBoss) {
            // Apply rank-based boss bonus
            Rank playerRank = plugin.getRankManager().getPlayerRank(killer.getUniqueId());
            Rank.RankCurrencySettings mobcoinSettings = playerRank.getMobcoinSettings();
            bonusPercent = mobcoinSettings.getBossKillBonus();
            
            if (bonusPercent > 0) {
                double bonus = NumberFormatter.truncateDecimal(reward * (bonusPercent / 100.0));
                finalReward = NumberFormatter.truncateDecimal(reward + bonus);
            }
        }
        
        // Add MobCoins to player
        plugin.getCurrencyManager().addBalance(killer.getUniqueId(), CurrencyType.MOBCOIN, finalReward);
        
        // Send notification
        String mobName = formatMobName(entityType);
        String symbol = plugin.getCurrencyManager().getCurrencySymbol(CurrencyType.MOBCOIN);
        
        if (isBoss) {
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "mob", mobName,
                    "amount", NumberFormatter.formatShort(finalReward),
                    "bonus", String.valueOf((int) bonusPercent));
            sendMessage(killer, "mob-kill.reward-boss", placeholders);
        } else {
            Map<String, String> placeholders = MessagesUtil.placeholders(
                    "mob", mobName,
                    "amount", NumberFormatter.formatShort(finalReward));
            sendMessage(killer, "mob-kill.reward", placeholders);
        }
    }
    
    /**
     * Get reward amount for a mob type
     */
    private double getMobReward(EntityType entityType) {
        ConfigurationSection mobRewards = plugin.getConfigManager().getMobRewards();
        
        // Check neutral mobs
        if (mobRewards.getBoolean("neutral.enabled", true)) {
            List<String> neutralMobs = mobRewards.getStringList("neutral.mobs");
            if (neutralMobs.contains(entityType.name())) {
                return mobRewards.getDouble("neutral.reward", 1.0);
            }
        }
        
        // Check easy mobs
        if (mobRewards.getBoolean("easy.enabled", true)) {
            List<String> easyMobs = mobRewards.getStringList("easy.mobs");
            if (easyMobs.contains(entityType.name())) {
                return mobRewards.getDouble("easy.reward", 2.0);
            }
        }
        
        // Check hard mobs
        if (mobRewards.getBoolean("hard.enabled", true)) {
            List<String> hardMobs = mobRewards.getStringList("hard.mobs");
            if (hardMobs.contains(entityType.name())) {
                return mobRewards.getDouble("hard.reward", 4.0);
            }
        }
        
        // Check boss mobs
        if (mobRewards.getBoolean("boss.enabled", true)) {
            if (entityType == EntityType.ENDER_DRAGON && 
                mobRewards.getBoolean("boss.ender-dragon.enabled", true)) {
                return mobRewards.getDouble("boss.ender-dragon.reward", 50.0);
            } else if (entityType == EntityType.WITHER && 
                      mobRewards.getBoolean("boss.wither.enabled", true)) {
                return mobRewards.getDouble("boss.wither.reward", 50.0);
            } else if (entityType == EntityType.WARDEN && 
                      mobRewards.getBoolean("boss.warden.enabled", true)) {
                return mobRewards.getDouble("boss.warden.reward", 50.0);
            }
        }
        
        return 0.0;
    }
    
    /**
     * Check if entity is a boss mob
     */
    private boolean isBossMob(EntityType entityType) {
        return entityType == EntityType.ENDER_DRAGON || 
               entityType == EntityType.WITHER || 
               entityType == EntityType.WARDEN;
    }
    
    /**
     * Format mob name for display
     */
    private String formatMobName(EntityType entityType) {
        String name = entityType.name().replace("_", " ").toLowerCase();
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                         .append(word.substring(1))
                         .append(" ");
            }
        }
        
        return formatted.toString().trim();
    }
    
    /**
     * Send a message with placeholders
     */
    private void sendMessage(Player player, String path, Map<String, String> placeholders) {
        String message = messageUtil.getMessage(path, placeholders);
        player.sendMessage(message);
    }
}