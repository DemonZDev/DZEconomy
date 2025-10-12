package online.demonzdevelopment.currency;

public enum CurrencyType {
    MONEY("money", "Money", "💵"),
    MOBCOIN("mobcoin", "MobCoin", "🪙"),
    GEM("gem", "Gem", "💎");

    private final String key;
    private final String displayName;
    private final String symbol;

    CurrencyType(String key, String displayName, String symbol) {
        this.key = key;
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public static CurrencyType fromString(String input) {
        if (input == null) return null;
        
        String normalized = input.toLowerCase().trim();
        for (CurrencyType type : values()) {
            if (type.key.equalsIgnoreCase(normalized) || 
                type.displayName.equalsIgnoreCase(normalized) ||
                type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }
}