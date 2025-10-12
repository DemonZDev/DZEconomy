package online.demonzdevelopment.listener;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.config.RankManager;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.util.FormatUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class MobKillListener implements Listener {
    private final DZEconomy plugin;

    public MobKillListener(DZEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        if (!plugin.getMobRewardsConfig().isEnabled()) {
            return;
        }
        
        if (!(event.getEntity().getKiller() instanceof Player killer)) {
            return;
        }
        
        EntityType entityType = event.getEntityType();
        BigDecimal baseReward = plugin.getMobRewardsConfig().getReward(entityType);
        
        if (baseReward.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(killer.getUniqueId());
        if (data == null) {
            return;
        }
        
        BigDecimal totalReward = baseReward;
        BigDecimal bonus = BigDecimal.ZERO;
        
        if (plugin.getMobRewardsConfig().isBossMob(entityType)) {
            RankManager.RankData rank = plugin.getRankManager().getPlayerRank(killer);
            if (rank.isBossKillBonusEnabled()) {
                BigDecimal bonusPercentage = rank.getBossKillBonus().divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN);
                bonus = baseReward.multiply(bonusPercentage).setScale(2, RoundingMode.DOWN);
                totalReward = totalReward.add(bonus);
            }
        }
        
        data.addBalance(CurrencyType.MOBCOIN, totalReward);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", formatAmount(baseReward));
        placeholders.put("mob", entityType.name().toLowerCase().replace("_", " "));
        
        killer.sendMessage(plugin.getMessageManager().getMessage("mob-kill.reward", placeholders));
        
        if (bonus.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, String> bonusPlaceholders = new HashMap<>();
            bonusPlaceholders.put("bonus", formatAmount(bonus));
            killer.sendMessage(plugin.getMessageManager().getMessage("mob-kill.boss-bonus", bonusPlaceholders));
        }
    }

    private String formatAmount(BigDecimal amount) {
        return FormatUtil.formatCurrency(
            amount,
            plugin.getConfigManager().useShortForm(),
            plugin.getConfigManager().getDecimalLimit()
        );
    }
}