package online.demonzdevelopment.manager;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.config.ConfigManager;
import online.demonzdevelopment.config.RankManager;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.data.PlayerDataManager;
import online.demonzdevelopment.util.FormatUtil;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class EconomyManager {
    private final DZEconomy plugin;
    private final ConfigManager configManager;
    private final RankManager rankManager;
    private final PlayerDataManager playerDataManager;

    public EconomyManager(DZEconomy plugin, ConfigManager configManager, RankManager rankManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.rankManager = rankManager;
        this.playerDataManager = playerDataManager;
    }

    public BigDecimal calculateTax(Player player, CurrencyType currency, BigDecimal amount) {
        RankManager.RankData rank = rankManager.getPlayerRank(player);
        RankManager.CurrencySettings settings = rank.getCurrencySettings(currency);
        
        if (!settings.isTaxEnabled()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal taxRate = settings.getTaxPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN);
        return amount.multiply(taxRate).setScale(2, RoundingMode.DOWN);
    }

    public BigDecimal calculateConversionTax(Player player, BigDecimal amount) {
        RankManager.RankData rank = rankManager.getPlayerRank(player);
        
        if (!rank.isConversionTaxEnabled()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal taxRate = rank.getConversionTax().divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN);
        return amount.multiply(taxRate).setScale(2, RoundingMode.DOWN);
    }

    public long getCooldownRemaining(Player player, CurrencyType currency) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        if (data == null) return 0;
        
        RankManager.RankData rank = rankManager.getPlayerRank(player);
        RankManager.CurrencySettings settings = rank.getCurrencySettings(currency);
        
        if (!settings.isCooldownEnabled()) {
            return 0;
        }
        
        long lastSend = data.getLastSendTime(currency);
        long cooldown = settings.getSendCooldown();
        long elapsed = (System.currentTimeMillis() - lastSend) / 1000;
        
        return Math.max(0, cooldown - elapsed);
    }

    public boolean canSend(Player player, CurrencyType currency) {
        return getCooldownRemaining(player, currency) == 0;
    }

    public boolean hasReachedSendLimit(Player player, CurrencyType currency) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        if (data == null) return false;
        
        RankManager.RankData rank = rankManager.getPlayerRank(player);
        RankManager.CurrencySettings settings = rank.getCurrencySettings(currency);
        
        if (!settings.isTimesSendLimitEnabled()) {
            return false;
        }
        
        return data.getDailySendCount(currency) >= settings.getTimesSendLimit();
    }

    public boolean isAmountValid(Player player, CurrencyType currency, BigDecimal amount) {
        RankManager.RankData rank = rankManager.getPlayerRank(player);
        RankManager.CurrencySettings settings = rank.getCurrencySettings(currency);
        
        if (settings.isMinSendLimitEnabled() && amount.compareTo(settings.getMinSendLimit()) < 0) {
            return false;
        }
        
        if (settings.isMaxSendLimitEnabled() && amount.compareTo(settings.getMaxSendLimit()) > 0) {
            return false;
        }
        
        return true;
    }

    public BigDecimal getMinSendLimit(Player player, CurrencyType currency) {
        RankManager.RankData rank = rankManager.getPlayerRank(player);
        return rank.getCurrencySettings(currency).getMinSendLimit();
    }

    public BigDecimal getMaxSendLimit(Player player, CurrencyType currency) {
        RankManager.RankData rank = rankManager.getPlayerRank(player);
        return rank.getCurrencySettings(currency).getMaxSendLimit();
    }

    public int getTimesSendLimit(Player player, CurrencyType currency) {
        RankManager.RankData rank = rankManager.getPlayerRank(player);
        return rank.getCurrencySettings(currency).getTimesSendLimit();
    }

    public BigDecimal convert(CurrencyType from, CurrencyType to, BigDecimal amount) {
        if (from == to) {
            return amount;
        }
        
        BigDecimal rate = getConversionRate(from, to);
        return amount.multiply(rate).setScale(2, RoundingMode.DOWN);
    }

    private BigDecimal getConversionRate(CurrencyType from, CurrencyType to) {
        if (from == CurrencyType.MONEY && to == CurrencyType.MOBCOIN) {
            return BigDecimal.ONE.divide(configManager.getConversionRate("mobcoin", "money"), 10, RoundingMode.DOWN);
        } else if (from == CurrencyType.MOBCOIN && to == CurrencyType.MONEY) {
            return configManager.getConversionRate("mobcoin", "money");
        } else if (from == CurrencyType.MOBCOIN && to == CurrencyType.GEM) {
            return BigDecimal.ONE.divide(configManager.getConversionRate("gem", "mobcoin"), 10, RoundingMode.DOWN);
        } else if (from == CurrencyType.GEM && to == CurrencyType.MOBCOIN) {
            return configManager.getConversionRate("gem", "mobcoin");
        } else if (from == CurrencyType.MONEY && to == CurrencyType.GEM) {
            return BigDecimal.ONE.divide(configManager.getConversionRate("gem", "money"), 10, RoundingMode.DOWN);
        } else if (from == CurrencyType.GEM && to == CurrencyType.MONEY) {
            return configManager.getConversionRate("gem", "money");
        }
        
        return BigDecimal.ONE;
    }

    public String formatCurrency(BigDecimal amount) {
        return FormatUtil.formatCurrency(amount, configManager.useShortForm(), configManager.getDecimalLimit());
    }
}