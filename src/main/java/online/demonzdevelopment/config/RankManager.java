package online.demonzdevelopment.config;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class RankManager {
    private final DZEconomy plugin;
    private FileConfiguration ranksConfig;
    private final Map<String, RankData> ranks;

    public RankManager(DZEconomy plugin) {
        this.plugin = plugin;
        this.ranks = new HashMap<>();
    }

    public void load() {
        File ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        ranks.clear();
        
        ConfigurationSection ranksSection = ranksConfig.getConfigurationSection("ranks");
        if (ranksSection != null) {
            for (String rankName : ranksSection.getKeys(false)) {
                ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankName);
                if (rankSection != null) {
                    ranks.put(rankName, new RankData(rankName, rankSection));
                }
            }
        }
    }

    public RankData getPlayerRank(Player player) {
        if (plugin.isLuckPermsHooked()) {
            for (RankData rank : ranks.values()) {
                if (player.hasPermission(rank.getPermission())) {
                    return rank;
                }
            }
        }
        return ranks.getOrDefault("default", new RankData("default", ranksConfig.getConfigurationSection("ranks.default")));
    }

    public RankData getRank(String name) {
        return ranks.get(name);
    }

    public static class RankData {
        private final String name;
        private final String permission;
        private final Map<CurrencyType, CurrencySettings> currencySettings;
        private final BigDecimal bossKillBonus;
        private final boolean bossKillBonusEnabled;
        private final BigDecimal conversionTax;
        private final boolean conversionTaxEnabled;

        public RankData(String name, ConfigurationSection section) {
            this.name = name;
            this.permission = section.getString("permission", "dzeconomy.default");
            this.currencySettings = new HashMap<>();
            
            for (CurrencyType type : CurrencyType.values()) {
                ConfigurationSection currencySection = section.getConfigurationSection(type.getKey());
                if (currencySection != null) {
                    currencySettings.put(type, new CurrencySettings(currencySection));
                } else {
                    currencySettings.put(type, new CurrencySettings(null));
                }
            }
            
            ConfigurationSection bossSection = section.getConfigurationSection("boss-kill-bonus");
            this.bossKillBonusEnabled = bossSection != null && bossSection.getBoolean("enable", true);
            this.bossKillBonus = BigDecimal.valueOf(bossSection != null ? bossSection.getDouble("percentage", 0.0) : 0.0);
            
            ConfigurationSection conversionSection = section.getConfigurationSection("conversion-tax");
            this.conversionTaxEnabled = conversionSection != null && conversionSection.getBoolean("enable", true);
            this.conversionTax = BigDecimal.valueOf(conversionSection != null ? conversionSection.getDouble("percentage", 5.0) : 5.0);
        }

        public String getName() {
            return name;
        }

        public String getPermission() {
            return permission;
        }

        public CurrencySettings getCurrencySettings(CurrencyType type) {
            return currencySettings.getOrDefault(type, new CurrencySettings(null));
        }

        public BigDecimal getBossKillBonus() {
            return bossKillBonus;
        }

        public boolean isBossKillBonusEnabled() {
            return bossKillBonusEnabled;
        }

        public BigDecimal getConversionTax() {
            return conversionTax;
        }

        public boolean isConversionTaxEnabled() {
            return conversionTaxEnabled;
        }
    }

    public static class CurrencySettings {
        private final boolean enabled;
        private final BigDecimal taxPercentage;
        private final boolean taxEnabled;
        private final long sendCooldown;
        private final boolean cooldownEnabled;
        private final BigDecimal sendLimit;
        private final boolean sendLimitEnabled;
        private final String sendLimitDuration;
        private final BigDecimal maxSendLimit;
        private final boolean maxSendLimitEnabled;
        private final BigDecimal minSendLimit;
        private final boolean minSendLimitEnabled;
        private final int timesSendLimit;
        private final boolean timesSendLimitEnabled;
        private final String timesSendLimitDuration;

        public CurrencySettings(ConfigurationSection section) {
            if (section == null) {
                this.enabled = true;
                this.taxPercentage = BigDecimal.valueOf(5.0);
                this.taxEnabled = true;
                this.sendCooldown = 300;
                this.cooldownEnabled = true;
                this.sendLimit = BigDecimal.valueOf(5000);
                this.sendLimitEnabled = true;
                this.sendLimitDuration = "daily";
                this.maxSendLimit = BigDecimal.valueOf(1000);
                this.maxSendLimitEnabled = true;
                this.minSendLimit = BigDecimal.valueOf(100);
                this.minSendLimitEnabled = true;
                this.timesSendLimit = 5;
                this.timesSendLimitEnabled = true;
                this.timesSendLimitDuration = "daily";
                return;
            }
            
            this.enabled = section.getBoolean("enable", true);
            
            ConfigurationSection taxSection = section.getConfigurationSection("tax");
            this.taxEnabled = taxSection != null && taxSection.getBoolean("enable", true);
            this.taxPercentage = BigDecimal.valueOf(taxSection != null ? taxSection.getDouble("percentage", 5.0) : 5.0);
            
            ConfigurationSection cooldownSection = section.getConfigurationSection("send-cooldown");
            this.cooldownEnabled = cooldownSection != null && cooldownSection.getBoolean("enable", true);
            String cooldownStr = cooldownSection != null ? cooldownSection.getString("duration", "300s") : "300s";
            this.sendCooldown = parseDuration(cooldownStr);
            
            ConfigurationSection limitSection = section.getConfigurationSection("send-limit");
            this.sendLimitEnabled = limitSection != null && limitSection.getBoolean("enable", true);
            this.sendLimit = BigDecimal.valueOf(limitSection != null ? limitSection.getDouble("amount", 5000) : 5000);
            this.sendLimitDuration = limitSection != null ? limitSection.getString("duration", "daily") : "daily";
            
            ConfigurationSection maxSection = section.getConfigurationSection("max-send-limit");
            this.maxSendLimitEnabled = maxSection != null && maxSection.getBoolean("enable", true);
            this.maxSendLimit = BigDecimal.valueOf(maxSection != null ? maxSection.getDouble("amount", 1000) : 1000);
            
            ConfigurationSection minSection = section.getConfigurationSection("min-send-limit");
            this.minSendLimitEnabled = minSection != null && minSection.getBoolean("enable", true);
            this.minSendLimit = BigDecimal.valueOf(minSection != null ? minSection.getDouble("amount", 100) : 100);
            
            ConfigurationSection timesSection = section.getConfigurationSection("times-send-limit");
            this.timesSendLimitEnabled = timesSection != null && timesSection.getBoolean("enable", true);
            this.timesSendLimit = timesSection != null ? timesSection.getInt("time", 5) : 5;
            this.timesSendLimitDuration = timesSection != null ? timesSection.getString("duration", "daily") : "daily";
        }

        private long parseDuration(String duration) {
            if (duration.endsWith("s")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1));
            }
            return Long.parseLong(duration);
        }

        public boolean isEnabled() { return enabled; }
        public BigDecimal getTaxPercentage() { return taxPercentage; }
        public boolean isTaxEnabled() { return taxEnabled; }
        public long getSendCooldown() { return sendCooldown; }
        public boolean isCooldownEnabled() { return cooldownEnabled; }
        public BigDecimal getSendLimit() { return sendLimit; }
        public boolean isSendLimitEnabled() { return sendLimitEnabled; }
        public String getSendLimitDuration() { return sendLimitDuration; }
        public BigDecimal getMaxSendLimit() { return maxSendLimit; }
        public boolean isMaxSendLimitEnabled() { return maxSendLimitEnabled; }
        public BigDecimal getMinSendLimit() { return minSendLimit; }
        public boolean isMinSendLimitEnabled() { return minSendLimitEnabled; }
        public int getTimesSendLimit() { return timesSendLimit; }
        public boolean isTimesSendLimitEnabled() { return timesSendLimitEnabled; }
        public String getTimesSendLimitDuration() { return timesSendLimitDuration; }
    }
}