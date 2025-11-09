package online.demonzdevelopment.dzeconomy.listener;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player quit events
 */
public class PlayerQuitListener implements Listener {
    
    private final DZEconomy plugin;
    
    public PlayerQuitListener(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Update last seen time
        PlayerData data = plugin.getCurrencyManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setLastSeen(System.currentTimeMillis());
        }
        
        // Save and unload player data
        plugin.getCurrencyManager().unloadPlayerData(player.getUniqueId());
        
        // Remove from rank cache
        plugin.getRankManager().reloadPlayerRank(player.getUniqueId());
        
        // Close any open request GUIs
        plugin.getRequestGUIManager().closeGUI(player);
    }
}