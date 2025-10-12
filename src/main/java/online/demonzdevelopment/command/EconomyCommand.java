package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.util.FormatUtil;
import online.demonzdevelopment.util.NumberUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;

public class EconomyCommand implements CommandExecutor, TabCompleter {
    private final DZEconomy plugin;

    public EconomyCommand(DZEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("general.must-be-online"));
            return true;
        }
        
        if (!plugin.getConfigManager().isConversionEnabled()) {
            player.sendMessage(plugin.getMessageManager().getMessage("economy.convert.disabled"));
            return true;
        }
        
        if (args.length < 3) {
            showHelp(player);
            return true;
        }
        
        CurrencyType from = CurrencyType.fromString(args[0]);
        CurrencyType to = CurrencyType.fromString(args[1]);
        BigDecimal amount = NumberUtil.parse(args[2]);
        
        if (from == null || to == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.invalid-currency"));
            return true;
        }
        
        if (from == to) {
            player.sendMessage(plugin.getMessageManager().getMessage("economy.convert.same-currency"));
            return true;
        }
        
        if (amount == null || !NumberUtil.isPositive(amount)) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.invalid-amount"));
            return true;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.player-not-found"));
            return true;
        }
        
        BigDecimal tax = plugin.getEconomyManager().calculateConversionTax(player, amount);
        BigDecimal total = amount.add(tax);
        
        if (!data.hasBalance(from, total)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("from_currency", from.getDisplayName());
            placeholders.put("amount", formatAmount(total));
            placeholders.put("tax", formatAmount(tax));
            player.sendMessage(plugin.getMessageManager().getMessage("economy.convert.insufficient", placeholders));
            return true;
        }
        
        BigDecimal converted = plugin.getEconomyManager().convert(from, to, amount);
        
        data.subtractBalance(from, total);
        data.addBalance(to, converted);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("from_amount", formatAmount(amount));
        placeholders.put("from_currency", from.getDisplayName());
        placeholders.put("to_amount", formatAmount(converted));
        placeholders.put("to_currency", to.getDisplayName());
        placeholders.put("tax", formatAmount(tax));
        
        player.sendMessage(plugin.getMessageManager().getMessage("economy.convert.success", placeholders));
        
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(plugin.getMessageManager().getMessage("economy.help.header"));
        player.sendMessage(plugin.getMessageManager().getMessage("economy.help.usage"));
        player.sendMessage(plugin.getMessageManager().getMessage("economy.help.example"));
    }

    private String formatAmount(BigDecimal amount) {
        return FormatUtil.formatCurrency(
            amount,
            plugin.getConfigManager().useShortForm(),
            plugin.getConfigManager().getDecimalLimit()
        );
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 || args.length == 2) {
            return Arrays.asList("money", "mobcoin", "gem");
        }
        return Collections.emptyList();
    }
}