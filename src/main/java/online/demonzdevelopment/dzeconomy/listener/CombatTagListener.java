package online.demonzdevelopment.dzeconomy.listener;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.util.ColorUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Listener for combat tagging events
 * Tracks PVP and PvE combat for request GUI blocking
 * 
 * @author DemonZ Development
 * @version 1.2.0
 */
public class CombatTagListener implements Listener {
    
    private final DZEconomy plugin;
    
    public CombatTagListener(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle entity damage to track combat
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getCombatTagManager().isEnabled()) {
            return;
        }
        
        // Get the actual damager (handle projectiles)
        Entity damager = getDamager(event.getDamager());
        Entity victim = event.getEntity();
        
        // PVP Combat: Player hits Player
        if (damager instanceof Player && victim instanceof Player) {
            Player attacker = (Player) damager;
            Player defender = (Player) victim;
            
            // Tag both players in combat
            plugin.getCombatTagManager().tagPlayer(attacker.getUniqueId());
            plugin.getCombatTagManager().tagPlayer(defender.getUniqueId());
            
            // Notify players (optional - can be disabled in messages)
            if (plugin.getConfigManager().getMessages().getBoolean("combat.notify-on-tag", false)) {
                String message = plugin.getConfigManager().getMessages()
                    .getString("combat.tagged-pvp", "&c⚔ You are now in combat with {player}!");
                
                attacker.sendMessage(ColorUtil.translate(message.replace("{player}", defender.getName())));
                defender.sendMessage(ColorUtil.translate(message.replace("{player}", attacker.getName())));
            }
        }
        // PvE Combat: Player hits dangerous mob OR dangerous mob hits player
        else if (damager instanceof Player && plugin.getCombatTagManager().isDangerousMob(victim.getType())) {
            // Player attacking dangerous mob
            Player player = (Player) damager;
            plugin.getCombatTagManager().tagPlayer(player.getUniqueId());
            
            // Notify player
            if (plugin.getConfigManager().getMessages().getBoolean("combat.notify-on-tag", false)) {
                String message = plugin.getConfigManager().getMessages()
                    .getString("combat.tagged-pve", "&c⚔ You are now in combat!");
                player.sendMessage(ColorUtil.translate(message));
            }
        }
        else if (victim instanceof Player && plugin.getCombatTagManager().isDangerousMob(damager.getType())) {
            // Dangerous mob attacking player
            Player player = (Player) victim;
            plugin.getCombatTagManager().tagPlayer(player.getUniqueId());
            
            // Notify player
            if (plugin.getConfigManager().getMessages().getBoolean("combat.notify-on-tag", false)) {
                String message = plugin.getConfigManager().getMessages()
                    .getString("combat.tagged-pve", "&c⚔ You are now in combat!");
                player.sendMessage(ColorUtil.translate(message));
            }
        }
    }
    
    /**
     * Get the actual damager entity (handles projectiles)
     */
    private Entity getDamager(Entity damager) {
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            
            if (shooter instanceof Entity) {
                return (Entity) shooter;
            }
        }
        
        return damager;
    }
    
    /**
     * Remove combat tag when player quits
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getCombatTagManager().untagPlayer(event.getPlayer().getUniqueId());
    }
}
