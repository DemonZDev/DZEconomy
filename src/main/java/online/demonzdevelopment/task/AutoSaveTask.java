package online.demonzdevelopment.task;

import online.demonzdevelopment.DZEconomy;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task to automatically save all player data at regular intervals
 */
public class AutoSaveTask extends BukkitRunnable {
    
    private final DZEconomy plugin;
    
    public AutoSaveTask(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        plugin.getLogger().info("Auto-saving player data...");
        
        try {
            plugin.getCurrencyManager().saveAllPlayers();
            plugin.getLogger().info("Auto-save completed successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to auto-save player data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}