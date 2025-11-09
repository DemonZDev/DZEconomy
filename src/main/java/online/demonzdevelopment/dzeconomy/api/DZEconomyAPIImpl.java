package online.demonzdevelopment.dzeconomy.api;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.currency.CurrencyType;
import online.demonzdevelopment.dzeconomy.rank.Rank;
import online.demonzdevelopment.dzeconomy.util.NumberFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the DZEconomy API
 * 
 * @author DemonZ Development
 * @version 1.1.1
 */
public class DZEconomyAPIImpl implements DZEconomyAPI {
    
    private final DZEconomy plugin;
    
    public DZEconomyAPIImpl(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public double getBalance(UUID player, CurrencyType currency) {
        return plugin.getCurrencyManager().getBalance(player, currency);
    }
    
    @Override
    public boolean hasBalance(UUID player, CurrencyType currency, double amount) {
        return plugin.getCurrencyManager().hasBalance(player, currency, amount);
    }
    
    @Override
    public void addCurrency(UUID player, CurrencyType currency, double amount) {
        plugin.getCurrencyManager().addBalance(player, currency, amount);
    }
    
    @Override
    public void removeCurrency(UUID player, CurrencyType currency, double amount) {
        plugin.getCurrencyManager().removeBalance(player, currency, amount);
    }
    
    @Override
    public void setCurrency(UUID player, CurrencyType currency, double amount) {
        plugin.getCurrencyManager().setBalance(player, currency, amount);
    }
    
    @Override
    public boolean transferCurrency(UUID from, UUID to, CurrencyType currency, double amount) {
        // Check if sender has balance
        if (!hasBalance(from, currency, amount)) {
            return false;
        }
        
        // Perform transfer (no tax for API calls)
        removeCurrency(from, currency, amount);
        addCurrency(to, currency, amount);
        
        return true;
    }
    
    @Override
    public boolean convertCurrency(UUID player, CurrencyType from, CurrencyType to, double amount) {
        // Check if player has balance
        if (!hasBalance(player, from, amount)) {
            return false;
        }
        
        // Calculate conversion rate
        double conversionRate = getConversionRate(from, to);
        double convertedAmount = NumberFormatter.truncateDecimal(amount * conversionRate);
        
        // Perform conversion (no tax for API calls)
        removeCurrency(player, from, amount);
        addCurrency(player, to, convertedAmount);
        
        return true;
    }
    
    /**
     * Get conversion rate between two currencies
     */
    private double getConversionRate(CurrencyType from, CurrencyType to) {
        double gemToMobcoin = plugin.getConfigManager().getConfig().getDouble("conversion.rates.gem-to-mobcoin", 100.0);
        double gemToMoney = plugin.getConfigManager().getConfig().getDouble("conversion.rates.gem-to-money", 10000.0);
        double mobcoinToMoney = plugin.getConfigManager().getConfig().getDouble("conversion.rates.mobcoin-to-money", 100.0);
        
        if (from == CurrencyType.GEM && to == CurrencyType.MOBCOIN) {
            return gemToMobcoin;
        } else if (from == CurrencyType.GEM && to == CurrencyType.MONEY) {
            return gemToMoney;
        } else if (from == CurrencyType.MOBCOIN && to == CurrencyType.GEM) {
            return 1.0 / gemToMobcoin;
        } else if (from == CurrencyType.MOBCOIN && to == CurrencyType.MONEY) {
            return mobcoinToMoney;
        } else if (from == CurrencyType.MONEY && to == CurrencyType.GEM) {
            return 1.0 / gemToMoney;
        } else if (from == CurrencyType.MONEY && to == CurrencyType.MOBCOIN) {
            return 1.0 / mobcoinToMoney;
        }
        
        return 1.0;
    }
    
    @Override
    public Rank getPlayerRank(UUID player) {
        return plugin.getRankManager().getPlayerRank(player);
    }
    
    @Override
    public List<Rank> getAllRanks() {
        return new ArrayList<>(plugin.getRankManager().getAllRanks());
    }
    
    @Override
    public String formatCurrency(double amount, CurrencyType currency) {
        String symbol = plugin.getCurrencyManager().getCurrencySymbol(currency);
        return symbol + NumberFormatter.formatShort(amount);
    }
    
    @Override
    public String formatCurrencyShort(double amount, CurrencyType currency) {
        return NumberFormatter.formatShort(amount);
    }
}