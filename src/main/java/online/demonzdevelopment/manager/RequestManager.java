package online.demonzdevelopment.manager;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.config.MessageManager;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.request.PaymentRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RequestManager {
    private final DZEconomy plugin;
    private final EconomyManager economyManager;
    private final MessageManager messageManager;
    private final Map<UUID, PaymentRequest> pendingRequests;

    public RequestManager(DZEconomy plugin, EconomyManager economyManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.messageManager = messageManager;
        this.pendingRequests = new HashMap<>();
    }

    public void createRequest(Player requester, Player target, CurrencyType currency, BigDecimal amount) {
        PaymentRequest request = new PaymentRequest(requester.getUniqueId(), target.getUniqueId(), currency, amount);
        pendingRequests.put(target.getUniqueId(), request);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.remove(target.getUniqueId()) != null) {
                Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
                if (onlineTarget != null) {
                    Map<String, String> placeholders = new HashMap<>();
                    onlineTarget.sendMessage(messageManager.getMessage(currency.getKey() + ".request.timeout", placeholders));
                }
            }
        }, 120 * 20L);
    }

    public PaymentRequest getPendingRequest(UUID playerUUID) {
        return pendingRequests.get(playerUUID);
    }

    public void removePendingRequest(UUID playerUUID) {
        pendingRequests.remove(playerUUID);
    }

    public boolean hasPendingRequest(UUID playerUUID) {
        return pendingRequests.containsKey(playerUUID);
    }

    public void shutdown() {
        pendingRequests.clear();
    }
}