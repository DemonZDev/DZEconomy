package online.demonzdevelopment.listener;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.util.FormatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

public class PlayerJoinListener implements Listener {
    private final DZEconomy plugin;

    public PlayerJoinListener(DZEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        plugin.getPlayerDataManager().loadPlayerData(player.getUniqueId(), player.getName()).thenAccept(data -> {
            boolean isNewPlayer = !player.hasPlayedBefore();
            
            if (isNewPlayer) {
                sendWelcomeMessage(player, data);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId()).thenRun(() -> {
            plugin.getPlayerDataManager().unloadPlayerData(player.getUniqueId());
        });
    }

    private void sendWelcomeMessage(Player player, PlayerData data) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("money", FormatUtil.formatCurrency(
            data.getBalance(CurrencyType.MONEY),
            plugin.getConfigManager().useShortForm(),
            plugin.getConfigManager().getDecimalLimit()
        ));
        placeholders.put("mobcoin", FormatUtil.formatCurrency(
            data.getBalance(CurrencyType.MOBCOIN),
            plugin.getConfigManager().useShortForm(),
            plugin.getConfigManager().getDecimalLimit()
        ));
        placeholders.put("gem", FormatUtil.formatCurrency(
            data.getBalance(CurrencyType.GEM),
            plugin.getConfigManager().useShortForm(),
            plugin.getConfigManager().getDecimalLimit()
        ));
        
        player.sendMessage(plugin.getMessageManager().getMessage("general.new-player-bonus", placeholders));
    }
}