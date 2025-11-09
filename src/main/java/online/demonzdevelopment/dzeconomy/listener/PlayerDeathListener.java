package online.demonzdevelopment.dzeconomy.listener;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.currency.CurrencyType;
import online.demonzdevelopment.dzeconomy.util.ColorUtil;
import online.demonzdevelopment.dzeconomy.util.MessagesUtil;
import online.demonzdevelopment.dzeconomy.util.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;

/**
 * Handles player death events for PVP economy transfers
 */
public class PlayerDeathListener implements Listener {
    
    private final DZEconomy plugin;
    private final MessagesUtil messageUtil;
    
    public PlayerDeathListener(DZEconomy plugin) {
        this.plugin = plugin;
        this.messageUtil = new MessagesUtil(plugin);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Early exit checks (performance optimization)
        if (!plugin.getConfigManager().getConfig().getBoolean("pvp-economy.enabled", true)) {
            return;
        }
        
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Quick validation checks
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        
        // Get victim's balances
        double victimMoney = plugin.getCurrencyManager().getBalance(victim.getUniqueId(), CurrencyType.MONEY);
        double victimMobcoin = plugin.getCurrencyManager().getBalance(victim.getUniqueId(), CurrencyType.MOBCOIN);
        double victimGem = plugin.getCurrencyManager().getBalance(victim.getUniqueId(), CurrencyType.GEM);
        
        // Check if any currency to transfer
        if (victimMoney == 0 && victimMobcoin == 0 && victimGem == 0) {
            return;
        }
        
        // Transfer currencies based on config
        boolean transferMoney = plugin.getConfigManager().getConfig().getBoolean("pvp-economy.transfer-money", true);
        boolean transferMobcoins = plugin.getConfigManager().getConfig().getBoolean("pvp-economy.transfer-mobcoins", true);
        boolean transferGems = plugin.getConfigManager().getConfig().getBoolean("pvp-economy.transfer-gems", true);
        
        if (transferMoney && victimMoney > 0) {
            plugin.getCurrencyManager().setBalance(victim.getUniqueId(), CurrencyType.MONEY, 0);
            plugin.getCurrencyManager().addBalance(killer.getUniqueId(), CurrencyType.MONEY, victimMoney);
        }
        
        if (transferMobcoins && victimMobcoin > 0) {
            plugin.getCurrencyManager().setBalance(victim.getUniqueId(), CurrencyType.MOBCOIN, 0);
            plugin.getCurrencyManager().addBalance(killer.getUniqueId(), CurrencyType.MOBCOIN, victimMobcoin);
        }
        
        if (transferGems && victimGem > 0) {
            plugin.getCurrencyManager().setBalance(victim.getUniqueId(), CurrencyType.GEM, 0);
            plugin.getCurrencyManager().addBalance(killer.getUniqueId(), CurrencyType.GEM, victimGem);
        }
        
        // Save data
        plugin.getCurrencyManager().savePlayerDataAsync(victim.getUniqueId());
        plugin.getCurrencyManager().savePlayerDataAsync(killer.getUniqueId());
        
        // Get currency symbols
        String moneySymbol = plugin.getCurrencyManager().getCurrencySymbol(CurrencyType.MONEY);
        String mobcoinSymbol = plugin.getCurrencyManager().getCurrencySymbol(CurrencyType.MOBCOIN);
        String gemSymbol = plugin.getCurrencyManager().getCurrencySymbol(CurrencyType.GEM);
        
        // Send notifications
        Map<String, String> killerPlaceholders = MessagesUtil.placeholders(
                "player", victim.getName(),
                "money_symbol", moneySymbol,
                "money", transferMoney ? NumberFormatter.formatShort(victimMoney) : "0",
                "mobcoin_symbol", mobcoinSymbol,
                "mobcoin", transferMobcoins ? NumberFormatter.formatShort(victimMobcoin) : "0",
                "gem_symbol", gemSymbol,
                "gem", transferGems ? NumberFormatter.formatShort(victimGem) : "0");
        
        sendMessage(killer, "pvp.killer", killerPlaceholders);
        
        Map<String, String> victimPlaceholders = MessagesUtil.placeholders("player", killer.getName());
        sendMessage(victim, "pvp.victim", victimPlaceholders);
        
        // Broadcast if enabled and threshold met
        if (plugin.getConfigManager().getConfig().getBoolean("pvp-economy.broadcast.enabled", true)) {
            double threshold = plugin.getConfigManager().getConfig().getDouble("pvp-economy.broadcast.threshold", 100000.0);
            
            if (victimMoney >= threshold) {
                Map<String, String> broadcastPlaceholders = MessagesUtil.placeholders(
                        "killer", killer.getName(),
                        "victim", victim.getName(),
                        "money_symbol", moneySymbol,
                        "money", NumberFormatter.formatShort(victimMoney),
                        "mobcoin_symbol", mobcoinSymbol,
                        "mobcoin", NumberFormatter.formatShort(victimMobcoin),
                        "gem_symbol", gemSymbol,
                        "gem", NumberFormatter.formatShort(victimGem));
                
                String broadcast = messageUtil.getMessage("pvp.broadcast", broadcastPlaceholders);
                Bukkit.broadcastMessage(broadcast);
            }
        }
    }
    
    /**
     * Send a message with placeholders
     */
    private void sendMessage(Player player, String path, Map<String, String> placeholders) {
        String message = messageUtil.getMessage(path, placeholders);
        player.sendMessage(message);
    }
}