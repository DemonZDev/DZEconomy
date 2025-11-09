package online.demonzdevelopment.dzeconomy.task;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.data.CurrencyRequest;
import online.demonzdevelopment.dzeconomy.util.ColorUtil;
import online.demonzdevelopment.dzeconomy.util.MessagesUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Task to check for and handle expired currency requests
 */
public class RequestTimeoutTask extends BukkitRunnable {
    
    private final DZEconomy plugin;
    
    public RequestTimeoutTask(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        int timeoutSeconds = plugin.getConfigManager().getConfig().getInt("limits.request-timeout", 120);
        Map<UUID, List<CurrencyRequest>> allRequests = plugin.getCurrencyManager().getAllPendingRequests();
        
        List<UUID> toRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, List<CurrencyRequest>> entry : allRequests.entrySet()) {
            UUID requestedPlayerUUID = entry.getKey();
            List<CurrencyRequest> requests = entry.getValue();
            
            if (requests == null || requests.isEmpty()) {
                continue;
            }
            
            // Check first request (FIFO)
            CurrencyRequest request = requests.get(0);
            
            if (request.isExpired(timeoutSeconds)) {
                // Remove expired request
                toRemove.add(requestedPlayerUUID);
                
                // Notify players
                Player requestedPlayer = Bukkit.getPlayer(requestedPlayerUUID);
                if (requestedPlayer != null && requestedPlayer.isOnline()) {
                    MessagesUtil messageUtil = new MessagesUtil(plugin);
                    Map<String, String> placeholders = MessagesUtil.placeholders(
                            "player", Bukkit.getOfflinePlayer(request.getRequesterUUID()).getName());
                    String message = messageUtil.getMessage("request.expired", placeholders);
                    requestedPlayer.sendMessage(message);
                    
                    // Close GUI if open
                    plugin.getRequestGUIManager().closeGUI(requestedPlayer);
                }
                
                Player requester = Bukkit.getPlayer(request.getRequesterUUID());
                if (requester != null && requester.isOnline()) {
                    MessagesUtil messageUtil = new MessagesUtil(plugin);
                    Map<String, String> placeholders = MessagesUtil.placeholders(
                            "player", Bukkit.getOfflinePlayer(requestedPlayerUUID).getName());
                    String message = messageUtil.getMessage("request.expired-other", placeholders);
                    requester.sendMessage(message);
                }
            }
        }
        
        // Remove expired requests
        for (UUID uuid : toRemove) {
            plugin.getCurrencyManager().removeRequest(uuid);
        }
    }
}