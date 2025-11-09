package online.demonzdevelopment.dzeconomy.task;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.data.PlayerData;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Task to reset daily limits at configured time
 */
public class DailyResetTask extends BukkitRunnable {
    
    private final DZEconomy plugin;
    private boolean hasResetToday = false;
    
    public DailyResetTask(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        try {
            // Get configured reset time (default: 00:00)
            String resetTimeStr = plugin.getConfigManager().getConfig().getString("daily-reset.time", "00:00");
            LocalTime resetTime = LocalTime.parse(resetTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime currentTime = LocalTime.now();
            
            // Check if it's time to reset
            if (isTimeToReset(currentTime, resetTime)) {
                if (!hasResetToday) {
                    performDailyReset();
                    hasResetToday = true;
                }
            } else {
                // Reset the flag when we're past the reset time
                hasResetToday = false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in daily reset task: " + e.getMessage());
        }
    }
    
    /**
     * Check if current time matches reset time (within 1 minute window)
     */
    private boolean isTimeToReset(LocalTime current, LocalTime reset) {
        int currentMinutes = current.getHour() * 60 + current.getMinute();
        int resetMinutes = reset.getHour() * 60 + reset.getMinute();
        
        return Math.abs(currentMinutes - resetMinutes) <= 1;
    }
    
    /**
     * Perform daily reset for all cached players
     */
    private void performDailyReset() {
        plugin.getLogger().info("Performing daily limit reset...");
        
        int resetCount = 0;
        for (PlayerData data : plugin.getCurrencyManager().getAllPlayerData()) {
            data.resetDailyLimits();
            plugin.getCurrencyManager().savePlayerDataAsync(data.getUUID());
            resetCount++;
        }
        
        plugin.getLogger().info("Daily reset completed! Reset " + resetCount + " player(s)");
    }
}