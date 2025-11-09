package online.demonzdevelopment.dzeconomy.task;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.update.UpdateChecker;
import online.demonzdevelopment.dzeconomy.update.UpdateManager;
import online.demonzdevelopment.dzeconomy.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runtime update checker task that runs periodically
 * 
 * @author DemonZ Development
 * @version 1.2.0
 */
public class RuntimeUpdateCheckTask extends BukkitRunnable {
    
    private final DZEconomy plugin;
    private final UpdateChecker updateChecker;
    
    public RuntimeUpdateCheckTask(DZEconomy plugin) {
        this.plugin = plugin;
        this.updateChecker = plugin.getUpdateChecker();
    }
    
    @Override
    public void run() {
        // Check if runtime update checking is enabled
        if (!plugin.getConfigManager().getConfig().getBoolean("update-checker.runtime-check-enabled", true)) {
            return;
        }
        
        // Run update check asynchronously
        updateChecker.checkForUpdates().thenAccept(hasUpdate -> {
            if (hasUpdate) {
                plugin.getLogger().info("New update available: v" + updateChecker.getLatestVersion());
                
                // Notify online admins
                if (plugin.getConfigManager().getConfig().getBoolean("update-checker.notify-on-join", true)) {
                    String message = updateChecker.getUpdateMessage();
                    if (message != null) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.hasPermission("dzeconomy.admin")) {
                                player.sendMessage(message);
                            }
                        }
                    }
                }
                
                // Auto-update if enabled
                if (plugin.getConfigManager().getConfig().getBoolean("updater.runtime-auto-update", false)) {
                    plugin.getLogger().info("Runtime auto-update is enabled. Downloading update...");
                    UpdateManager updateManager = new UpdateManager(plugin);
                    updateManager.autoUpdate().thenAccept(result -> {
                        if (result.isSuccess()) {
                            plugin.getLogger().info("Update downloaded! Restart server to apply v" + result.getVersion());
                            
                            // Notify admins
                            String adminMessage = ColorUtil.translate("&8[&6DZ&eEconomy&8] &aUpdate downloaded! &eRestart server to apply v" + result.getVersion());
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (player.hasPermission("dzeconomy.admin")) {
                                    player.sendMessage(adminMessage);
                                }
                            }
                        }
                    });
                }
            }
        });
    }
}
