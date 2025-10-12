package online.demonzdevelopment.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.util.FormatUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DZEconomyExpansion extends PlaceholderExpansion {
    private final DZEconomy plugin;

    public DZEconomyExpansion(DZEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dz";
    }

    @Override
    public @NotNull String getAuthor() {
        return "DemonzDevelopment";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            return "0";
        }
        
        return switch (params.toLowerCase()) {
            case "money" -> formatBalance(data.getBalance(CurrencyType.MONEY));
            case "mobcoin" -> formatBalance(data.getBalance(CurrencyType.MOBCOIN));
            case "gem" -> formatBalance(data.getBalance(CurrencyType.GEM));
            case "rank" -> plugin.getRankManager().getPlayerRank(player).getName();
            default -> null;
        };
    }

    private String formatBalance(java.math.BigDecimal balance) {
        return FormatUtil.formatCurrency(
            balance,
            plugin.getConfigManager().useShortForm(),
            plugin.getConfigManager().getDecimalLimit()
        );
    }
}