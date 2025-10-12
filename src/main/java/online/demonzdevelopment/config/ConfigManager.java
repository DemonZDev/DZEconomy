package online.demonzdevelopment.config;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;

public class ConfigManager {
    private final DZEconomy plugin;
    private FileConfiguration config;

    public ConfigManager(DZEconomy plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public boolean isDebugMode() {
        return config.getBoolean("debug", false);
    }

    public int getAutoSaveInterval() {
        return config.getInt("auto-save-interval", 5);
    }

    public boolean isDatabaseEnabled() {
        return config.getBoolean("database.enable", false);
    }

    public String getDatabaseType() {
        return config.getString("database.type", "MySQL");
    }

    public String getDatabaseHost() {
        return config.getString("database.ip", "localhost");
    }

    public int getDatabasePort() {
        return config.getInt("database.port", 3306);
    }

    public String getDatabaseName() {
        return config.getString("database.database", "dzeconomy");
    }

    public String getDatabaseUsername() {
        return config.getString("database.username", "root");
    }

    public String getDatabasePassword() {
        return config.getString("database.password", "");
    }

    public boolean isCurrencyEnabled(CurrencyType type) {
        String path = "currencies." + type.getKey() + ".enable";
        return config.getBoolean(path, true);
    }

    public BigDecimal getNewPlayerBonus(CurrencyType type) {
        String path = "currencies." + type.getKey() + ".new_player_bonus";
        return BigDecimal.valueOf(config.getDouble(path, 0));
    }

    public String getCurrencySymbol(CurrencyType type) {
        String path = "currencies." + type.getKey() + ".symbol";
        return config.getString(path, type.getSymbol());
    }

    public int getDecimalPlaces(CurrencyType type) {
        String path = "currencies." + type.getKey() + ".decimal_places";
        return config.getInt(path, 2);
    }

    public boolean isConversionEnabled() {
        return config.getBoolean("conversion.enabled", true);
    }

    public BigDecimal getConversionRate(String from, String to) {
        String key = from.toLowerCase() + "-to-" + to.toLowerCase();
        String path = "conversion.rates." + key;
        return BigDecimal.valueOf(config.getDouble(path, 1.0));
    }

    public boolean useShortForm() {
        return config.getBoolean("format.use-short-form", true);
    }

    public int getDecimalLimit() {
        return config.getInt("format.decimal-limit", 2);
    }

    public boolean isPvpEconomyEnabled() {
        return config.getBoolean("pvp-economy.enabled", true);
    }

    public boolean isPlaceholderAPIEnabled() {
        return config.getBoolean("hooks.placeholderapi", true);
    }

    public boolean isLuckPermsEnabled() {
        return config.getBoolean("hooks.luckperms", true);
    }
}