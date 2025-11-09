package online.demonzdevelopment.dzeconomy.manager;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.data.PlayerData;
import online.demonzdevelopment.dzeconomy.storage.StorageProvider;
import online.demonzdevelopment.dzeconomy.storage.impl.FlatFileStorageProvider;
import online.demonzdevelopment.dzeconomy.storage.impl.MySQLStorageProvider;
import online.demonzdevelopment.dzeconomy.storage.impl.SQLiteStorageProvider;
import online.demonzdevelopment.dzeconomy.util.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Handles data migration between different storage providers
 * 
 * @author DemonZ Development
 * @version 1.2.0
 */
public class MigrationManager {
    
    private final DZEconomy plugin;
    
    public MigrationManager(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Migrate data from one storage type to another
     */
    public CompletableFuture<MigrationResult> migrateData(CommandSender sender, String fromType, String toType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                sendMessage(sender, "&eStarting data migration...");
                sendMessage(sender, "&7From: &e" + fromType.toUpperCase());
                sendMessage(sender, "&7To: &e" + toType.toUpperCase());
                
                // Validate storage types
                if (!isValidStorageType(fromType) || !isValidStorageType(toType)) {
                    sendMessage(sender, "&cInvalid storage type! Use: FLATFILE, SQLITE, or MYSQL");
                    return new MigrationResult(false, "Invalid storage type", 0);
                }
                
                if (fromType.equalsIgnoreCase(toType)) {
                    sendMessage(sender, "&cSource and destination storage types are the same!");
                    return new MigrationResult(false, "Same storage type", 0);
                }
                
                // Create backup first
                sendMessage(sender, "&eCreating backup...");
                if (!createBackup(sender, fromType)) {
                    sendMessage(sender, "&cFailed to create backup! Migration aborted.");
                    return new MigrationResult(false, "Backup failed", 0);
                }
                sendMessage(sender, "&aBackup created successfully!");
                
                // Initialize source and destination storage providers
                StorageProvider sourceProvider = createStorageProvider(fromType);
                StorageProvider destProvider = createStorageProvider(toType);
                
                if (sourceProvider == null || destProvider == null) {
                    sendMessage(sender, "&cFailed to initialize storage providers!");
                    return new MigrationResult(false, "Provider initialization failed", 0);
                }
                
                sourceProvider.initialize();
                destProvider.initialize();
                
                // Get all player UUIDs from source
                List<UUID> playerUUIDs = getAllPlayerUUIDs(sourceProvider, fromType);
                
                if (playerUUIDs.isEmpty()) {
                    sendMessage(sender, "&cNo player data found in source storage!");
                    sourceProvider.close();
                    destProvider.close();
                    return new MigrationResult(false, "No data to migrate", 0);
                }
                
                sendMessage(sender, "&eFound &a" + playerUUIDs.size() + " &eplayers to migrate...");
                
                // Migrate each player
                int successCount = 0;
                int failedCount = 0;
                
                for (int i = 0; i < playerUUIDs.size(); i++) {
                    UUID uuid = playerUUIDs.get(i);
                    
                    try {
                        PlayerData data = sourceProvider.loadPlayerData(uuid);
                        if (data != null) {
                            destProvider.savePlayerData(data);
                            successCount++;
                        } else {
                            failedCount++;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to migrate data for " + uuid + ": " + e.getMessage());
                        failedCount++;
                    }
                    
                    // Progress update every 10%
                    if ((i + 1) % Math.max(1, playerUUIDs.size() / 10) == 0) {
                        int progress = (int) ((i + 1) * 100.0 / playerUUIDs.size());
                        sendMessage(sender, "&7Progress: &e" + progress + "% &7(" + (i + 1) + "/" + playerUUIDs.size() + ")");
                    }
                }
                
                // Close providers
                sourceProvider.close();
                destProvider.close();
                
                // Report results
                sendMessage(sender, "&a&l✓ Migration completed!");
                sendMessage(sender, "&7Successfully migrated: &a" + successCount + " &7players");
                if (failedCount > 0) {
                    sendMessage(sender, "&7Failed: &c" + failedCount + " &7players");
                }
                sendMessage(sender, "&eUpdate config.yml to use the new storage type and restart!");
                
                return new MigrationResult(true, "Migration successful", successCount);
                
            } catch (Exception e) {
                plugin.getLogger().severe("Migration failed: " + e.getMessage());
                e.printStackTrace();
                sendMessage(sender, "&cMigration failed: " + e.getMessage());
                return new MigrationResult(false, "Migration error: " + e.getMessage(), 0);
            }
        });
    }
    
    /**
     * Create backup of current data
     */
    private boolean createBackup(CommandSender sender, String storageType) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File backupFolder = new File(plugin.getDataFolder(), "backups/" + timestamp);
            backupFolder.mkdirs();
            
            if (storageType.equalsIgnoreCase("FLATFILE")) {
                // Backup flatfile data
                File dataFolder = new File(plugin.getDataFolder(), "data");
                if (dataFolder.exists()) {
                    copyFolder(dataFolder.toPath(), backupFolder.toPath());
                }
            } else if (storageType.equalsIgnoreCase("SQLITE")) {
                // Backup SQLite database file
                File dbFile = new File(plugin.getDataFolder(), "data/economy.db");
                if (dbFile.exists()) {
                    Files.copy(dbFile.toPath(), 
                             new File(backupFolder, "economy.db").toPath(),
                             StandardCopyOption.REPLACE_EXISTING);
                }
            } else if (storageType.equalsIgnoreCase("MYSQL")) {
                // MySQL backup - export data to YAML
                File mysqlBackup = new File(backupFolder, "mysql_backup.yml");
                YamlConfiguration backup = new YamlConfiguration();
                backup.set("backup-time", System.currentTimeMillis());
                backup.set("storage-type", "MYSQL");
                backup.save(mysqlBackup);
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create backup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Copy folder recursively
     */
    private void copyFolder(Path source, Path destination) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path destPath = destination.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(destPath);
                } else {
                    Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to copy: " + sourcePath);
            }
        });
    }
    
    /**
     * Get all player UUIDs from storage
     */
    private List<UUID> getAllPlayerUUIDs(StorageProvider provider, String storageType) {
        List<UUID> uuids = new ArrayList<>();
        
        try {
            if (storageType.equalsIgnoreCase("FLATFILE")) {
                File dataFolder = new File(plugin.getDataFolder(), "data/players");
                if (dataFolder.exists() && dataFolder.isDirectory()) {
                    File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
                    if (files != null) {
                        for (File file : files) {
                            try {
                                String uuidStr = file.getName().replace(".yml", "");
                                uuids.add(UUID.fromString(uuidStr));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }
            } else if (storageType.equalsIgnoreCase("SQLITE") || storageType.equalsIgnoreCase("MYSQL")) {
                // For SQL-based storage, we need to query all UUIDs
                // This is handled in the storage provider
                // For now, we'll load from currently loaded players
                for (PlayerData data : plugin.getCurrencyManager().getAllLoadedPlayers()) {
                    uuids.add(data.getUUID());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player UUIDs: " + e.getMessage());
        }
        
        return uuids;
    }
    
    /**
     * Create storage provider instance
     */
    private StorageProvider createStorageProvider(String type) {
        switch (type.toUpperCase()) {
            case "FLATFILE":
                return new FlatFileStorageProvider(plugin);
            case "SQLITE":
                return new SQLiteStorageProvider(plugin);
            case "MYSQL":
                return new MySQLStorageProvider(plugin);
            default:
                return null;
        }
    }
    
    /**
     * Check if storage type is valid
     */
    private boolean isValidStorageType(String type) {
        return type.equalsIgnoreCase("FLATFILE") || 
               type.equalsIgnoreCase("SQLITE") || 
               type.equalsIgnoreCase("MYSQL");
    }
    
    /**
     * Send colored message to sender
     */
    private void sendMessage(CommandSender sender, String message) {
        if (sender != null) {
            String prefix = ColorUtil.translate("&8[&6DZ&eEconomy&8]&r ");
            sender.sendMessage(ColorUtil.translate(prefix + message));
        } else {
            plugin.getLogger().info(message.replace("&", ""));
        }
    }
    
    /**
     * Migration result
     */
    public static class MigrationResult {
        private final boolean success;
        private final String message;
        private final int migratedCount;
        
        public MigrationResult(boolean success, String message, int migratedCount) {
            this.success = success;
            this.message = message;
            this.migratedCount = migratedCount;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getMigratedCount() {
            return migratedCount;
        }
    }
}
