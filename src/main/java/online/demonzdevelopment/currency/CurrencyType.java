package online.demonzdevelopment.currency;

/**
 * Enum representing the three currency types in DZEconomy
 */
public enum CurrencyType {
    MONEY("money", "Money", "&aMoney", "$"),
    MOBCOIN("mobcoin", "MobCoin", "&6MobCoin", "MC"),
    GEM("gem", "Gem", "&b&lGem", "◆");
    
    private final String id;
    private final String name;
    private final String displayName;
    private final String defaultSymbol;
    
    CurrencyType(String id, String name, String displayName, String defaultSymbol) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.defaultSymbol = defaultSymbol;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDefaultSymbol() {
        return defaultSymbol;
    }
    
    /**
     * Get currency type from string (case-insensitive)
     */
    public static CurrencyType fromString(String str) {
        if (str == null) return null;
        
        for (CurrencyType type : values()) {
            if (type.id.equalsIgnoreCase(str) || type.name.equalsIgnoreCase(str)) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * Get currency display color
     */
    public String getColor() {
        switch (this) {
            case MONEY:
                return "&a";
            case MOBCOIN:
                return "&6";
            case GEM:
                return "&b";
            default:
                return "&f";
        }
    }
}