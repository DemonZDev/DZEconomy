package online.demonzdevelopment.listener;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.util.FormatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class PlayerDeathListener implements Listener {
    private final DZEconomy plugin;

    public PlayerDeathListener(DZEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().isPvpEconomyEnabled()) {
            return;
        }
        
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        
        if (killer == null || killer.equals(victim)) {
            return;
        }
        
        PlayerData victimData = plugin.getPlayerDataManager().getPlayerData(victim.getUniqueId());
        PlayerData killerData = plugin.getPlayerDataManager().getPlayerData(killer.getUniqueId());
        
        if (victimData == null || killerData == null) {
            return;
        }
        
        BigDecimal moneyTransferred = victimData.getBalance(CurrencyType.MONEY);
        BigDecimal mobcoinTransferred = victimData.getBalance(CurrencyType.MOBCOIN);
        BigDecimal gemTransferred = victimData.getBalance(CurrencyType.GEM);
        
        victimData.setBalance(CurrencyType.MONEY, BigDecimal.ZERO);
        victimData.setBalance(CurrencyType.MOBCOIN, BigDecimal.ZERO);
        victimData.setBalance(CurrencyType.GEM, BigDecimal.ZERO);
        
        killerData.addBalance(CurrencyType.MONEY, moneyTransferred);
        killerData.addBalance(CurrencyType.MOBCOIN, mobcoinTransferred);
        killerData.addBalance(CurrencyType.GEM, gemTransferred);
        
        Map<String, String> killerPlaceholders = new HashMap<>();
        killerPlaceholders.put("player", victim.getName());
        killerPlaceholders.put("money", formatAmount(moneyTransferred));
        killerPlaceholders.put("mobcoin", formatAmount(mobcoinTransferred));
        killerPlaceholders.put("gems", formatAmount(gemTransferred));
        
        Map<String, String> victimPlaceholders = new HashMap<>();
        victimPlaceholders.put("player", killer.getName());
        
        killer.sendMessage(plugin.getMessageManager().getMessage("pvp.killer", killerPlaceholders));
        victim.sendMessage(plugin.getMessageManager().getMessage("pvp.victim", victimPlaceholders));
    }

    private String formatAmount(BigDecimal amount) {
        return FormatUtil.formatCurrency(
            amount,
            plugin.getConfigManager().useShortForm(),
            plugin.getConfigManager().getDecimalLimit()
        );
    }
}