package online.demonzdevelopment.rank;

import online.demonzdevelopment.currency.CurrencyType;

/**
 * Represents a rank with all its currency-specific settings
 */
public class Rank {
    
    private final String id;
    private final String displayName;
    private final int priority;
    
    // Per-currency settings
    private final RankCurrencySettings moneySettings;
    private final RankCurrencySettings mobcoinSettings;
    private final RankCurrencySettings gemSettings;
    
    // Conversion settings
    private final boolean conversionEnabled;
    private final double conversionTax;
    
    public Rank(String id, String displayName, int priority,
                RankCurrencySettings moneySettings,
                RankCurrencySettings mobcoinSettings,
                RankCurrencySettings gemSettings,
                boolean conversionEnabled, double conversionTax) {
        this.id = id;
        this.displayName = displayName;
        this.priority = priority;
        this.moneySettings = moneySettings;
        this.mobcoinSettings = mobcoinSettings;
        this.gemSettings = gemSettings;
        this.conversionEnabled = conversionEnabled;
        this.conversionTax = conversionTax;
    }
    
    /**
     * Get settings for a specific currency
     */
    public RankCurrencySettings getSettingsFor(CurrencyType currency) {
        switch (currency) {
            case MONEY:
                return moneySettings;
            case MOBCOIN:
                return mobcoinSettings;
            case GEM:
                return gemSettings;
            default:
                return moneySettings;
        }
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public RankCurrencySettings getMoneySettings() {
        return moneySettings;
    }
    
    public RankCurrencySettings getMobcoinSettings() {
        return mobcoinSettings;
    }
    
    public RankCurrencySettings getGemSettings() {
        return gemSettings;
    }
    
    public boolean isConversionEnabled() {
        return conversionEnabled;
    }
    
    public double getConversionTax() {
        return conversionTax;
    }
    
    /**
     * Inner class for currency-specific rank settings
     */
    public static class RankCurrencySettings {
        private final boolean enabled;
        private final double transferTax;
        private final int transferCooldown;
        private final int dailyTransferLimit;
        private final int dailyRequestLimit;
        private final int requestCooldown;
        private final double bossKillBonus; // Only for MobCoin
        
        public RankCurrencySettings(boolean enabled, double transferTax, int transferCooldown,
                                    int dailyTransferLimit, int dailyRequestLimit,
                                    int requestCooldown, double bossKillBonus) {
            this.enabled = enabled;
            this.transferTax = transferTax;
            this.transferCooldown = transferCooldown;
            this.dailyTransferLimit = dailyTransferLimit;
            this.dailyRequestLimit = dailyRequestLimit;
            this.requestCooldown = requestCooldown;
            this.bossKillBonus = bossKillBonus;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public double getTransferTax() {
            return transferTax;
        }
        
        public int getTransferCooldown() {
            return transferCooldown;
        }
        
        public int getDailyTransferLimit() {
            return dailyTransferLimit;
        }
        
        public int getDailyRequestLimit() {
            return dailyRequestLimit;
        }
        
        public int getRequestCooldown() {
            return requestCooldown;
        }
        
        public double getBossKillBonus() {
            return bossKillBonus;
        }
    }
}