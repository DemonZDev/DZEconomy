package online.demonzdevelopment.config;

import online.demonzdevelopment.DZEconomy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

public class MobRewardsConfig {
    private final DZEconomy plugin;
    private FileConfiguration config;
    private boolean enabled;
    private final Map<String, MobCategory> categories;

    public MobRewardsConfig(DZEconomy plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
    }

    public void load() {
        File configFile = new File(plugin.getDataFolder(), "mob-rewards.yml");
        if (!configFile.exists()) {
            plugin.saveResource("mob-rewards.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("enabled", true);
        categories.clear();
        
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String categoryName : categoriesSection.getKeys(false)) {
                ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryName);
                if (categorySection != null) {
                    categories.put(categoryName, new MobCategory(categoryName, categorySection));
                }
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public BigDecimal getReward(EntityType entityType) {
        for (MobCategory category : categories.values()) {
            if (category.isEnabled() && category.containsMob(entityType)) {
                return category.getReward();
            }
        }
        return BigDecimal.ZERO;
    }

    public boolean isBossMob(EntityType entityType) {
        MobCategory bossCategory = categories.get("boss");
        return bossCategory != null && bossCategory.containsMob(entityType);
    }

    public static class MobCategory {
        private final String name;
        private final boolean enabled;
        private final BigDecimal reward;
        private final Set<EntityType> mobs;

        public MobCategory(String name, ConfigurationSection section) {
            this.name = name;
            this.enabled = section.getBoolean("enable", true);
            this.reward = BigDecimal.valueOf(section.getDouble("reward", 1.0));
            this.mobs = new HashSet<>();
            
            List<String> mobList = section.getStringList("mobs");
            for (String mobName : mobList) {
                try {
                    EntityType type = EntityType.valueOf(mobName.toUpperCase());
                    mobs.add(type);
                } catch (IllegalArgumentException e) {
                }
            }
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public BigDecimal getReward() {
            return reward;
        }

        public boolean containsMob(EntityType type) {
            return mobs.contains(type);
        }
    }
}