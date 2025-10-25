package online.demonzdevelopment.event;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.util.ColorUtil;
import online.demonzdevelopment.util.MessagesUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;

/**
 * Handles player join events
 */
public class PlayerJoinListener implements Listener {
    
    private final DZEconomy plugin;
    private final MessagesUtil messageUtil;
    
    public PlayerJoinListener(DZEconomy plugin) {
        this.plugin = plugin;
        this.messageUtil = new MessagesUtil(plugin);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check for updates and notify admins
        if (plugin.getUpdateChecker() != null && 
            plugin.getConfigManager().getConfig().getBoolean("update-checker.notify-on-join", true) &&
            (player.hasPermission("dzeconomy.admin") || player.isOp())) {
            
            if (plugin.getUpdateChecker().isCheckComplete() && 
                plugin.getUpdateChecker().isUpdateAvailable()) {
                
                String updateMessage = plugin.getUpdateChecker().getUpdateMessage();
                if (updateMessage != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.sendMessage(updateMessage);
                    }, 40L); // 2 seconds delay
                }
            }
        }
        
        // Load player data
        PlayerData data = plugin.getCurrencyManager().loadPlayerData(player.getUniqueId());
        
        // Update username
        data.setUsername(player.getName());
        data.setLastSeen(System.currentTimeMillis());
        
        // Check if new player
        boolean isNewPlayer = !plugin.getStorageProvider().playerDataExists(player.getUniqueId()) || 
                             data.getFirstJoin() == 0;
        
        if (isNewPlayer) {
            data.setFirstJoin(System.currentTimeMillis());
            
            // Send welcome message if enabled
            if (plugin.getConfigManager().getConfig().getBoolean("welcome-message.enabled", true) &&
                plugin.getConfigManager().getConfig().getBoolean("welcome-message.new-player", true)) {
                
                Map<String, String> placeholders = MessagesUtil.placeholders("player", player.getName());
                String welcomeMessage = messageUtil.getMessage("welcome.new-player", placeholders);
                player.sendMessage(welcomeMessage);
            }
        }
        
        // Load rank
        plugin.getRankManager().getPlayerRank(player.getUniqueId());
        
        // Save data
        plugin.getCurrencyManager().savePlayerDataAsync(player.getUniqueId());
    }
}