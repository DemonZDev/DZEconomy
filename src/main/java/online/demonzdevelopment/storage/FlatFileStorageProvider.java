package online.demonzdevelopment.storage;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.data.PlayerData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FlatFileStorageProvider implements StorageProvider {
    private final DZEconomy plugin;
    private final File dataFolder;

    public FlatFileStorageProvider(DZEconomy plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
    }

    @Override
    public void initialize() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    @Override
    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(dataFolder, uuid.toString() + ".yml");
            if (!file.exists()) {
                return null;
            }
            
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                return PlayerData.deserialize(config.getValues(false));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load player data for " + uuid + ": " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            File file = new File(dataFolder, data.getUUID().toString() + ".yml");
            
            try {
                YamlConfiguration config = new YamlConfiguration();
                for (var entry : data.serialize().entrySet()) {
                    config.set(entry.getKey(), entry.getValue());
                }
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save player data for " + data.getUUID() + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(dataFolder, uuid.toString() + ".yml");
            return file.exists();
        });
    }

    @Override
    public CompletableFuture<UUID> getUUIDByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            File[] files = dataFolder.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
            if (files == null) return null;
            
            for (File file : files) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String playerName = config.getString("name");
                    if (playerName != null && playerName.equalsIgnoreCase(name)) {
                        String uuidString = file.getName().replace(".yml", "");
                        return UUID.fromString(uuidString);
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            return null;
        });
    }

    @Override
    public void close() {
    }
}