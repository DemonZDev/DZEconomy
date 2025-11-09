package online.demonzdevelopment.dzeconomy.task;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task to periodically clean up expired combat tags
 * Runs every 5 seconds to remove expired tags and free memory
 * 
 * @author DemonZ Development
 * @version 1.2.0
 */
public class CombatTagCleanupTask extends BukkitRunnable {
    
    private final DZEconomy plugin;
    
    public CombatTagCleanupTask(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        if (plugin.getCombatTagManager() != null) {
            plugin.getCombatTagManager().cleanupExpiredTags();
        }
    }
}
