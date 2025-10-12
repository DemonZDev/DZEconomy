package online.demonzdevelopment.data;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class DailyResetService {
    private final DZEconomy plugin;
    private final PlayerDataManager playerDataManager;
    private BukkitTask resetTask;
    private LocalDate lastResetDate;

    public DailyResetService(DZEconomy plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.lastResetDate = LocalDate.now();
    }

    public void start() {
        long delayTicks = calculateDelayToMidnight();
        long periodTicks = 24 * 60 * 60 * 20L;
        
        resetTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::performDailyReset, delayTicks, periodTicks);
        
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Daily reset service started. Next reset in " + (delayTicks / 20 / 60) + " minutes");
        }
    }

    private long calculateDelayToMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);
        long secondsUntilMidnight = ChronoUnit.SECONDS.between(now, midnight);
        return secondsUntilMidnight * 20L;
    }

    private void performDailyReset() {
        LocalDate today = LocalDate.now();
        
        if (today.isAfter(lastResetDate)) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Performing daily reset for all players");
            }
            
            for (PlayerData data : playerDataManager.getAllPlayerData()) {
                for (CurrencyType type : CurrencyType.values()) {
                    long resetTime = data.getDailyResetTime(type);
                    LocalDate resetDate = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(resetTime),
                        ZoneId.systemDefault()
                    ).toLocalDate();
                    
                    if (today.isAfter(resetDate)) {
                        data.resetDailySendCount(type);
                    }
                }
            }
            
            lastResetDate = today;
        }
    }

    public void shutdown() {
        if (resetTask != null) {
            resetTask.cancel();
        }
    }
}