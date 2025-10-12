package online.demonzdevelopment.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.config.ConfigManager;
import online.demonzdevelopment.data.PlayerData;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySQLStorageProvider implements StorageProvider {
    private final DZEconomy plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    public MySQLStorageProvider(DZEconomy plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void initialize() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + configManager.getDatabaseHost() + ":" + 
                         configManager.getDatabasePort() + "/" + configManager.getDatabaseName());
        config.setUsername(configManager.getDatabaseUsername());
        config.setPassword(configManager.getDatabasePassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        dataSource = new HikariDataSource(config);
        createTables();
    }

    private void createTables() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS dzeconomy_players (" +
            "uuid VARCHAR(36) PRIMARY KEY," +
            "name VARCHAR(16) NOT NULL," +
            "money DECIMAL(65,2) DEFAULT 0," +
            "mobcoin DECIMAL(65,2) DEFAULT 0," +
            "gem DECIMAL(65,2) DEFAULT 0," +
            "money_last_send BIGINT DEFAULT 0," +
            "mobcoin_last_send BIGINT DEFAULT 0," +
            "gem_last_send BIGINT DEFAULT 0," +
            "money_send_count INT DEFAULT 0," +
            "mobcoin_send_count INT DEFAULT 0," +
            "gem_send_count INT DEFAULT 0," +
            "money_reset_time BIGINT DEFAULT 0," +
            "mobcoin_reset_time BIGINT DEFAULT 0," +
            "gem_reset_time BIGINT DEFAULT 0" +
            ")";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT * FROM dzeconomy_players WHERE uuid = ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("uuid", rs.getString("uuid"));
                    data.put("name", rs.getString("name"));
                    
                    Map<String, String> balances = new HashMap<>();
                    balances.put("money", rs.getString("money"));
                    balances.put("mobcoin", rs.getString("mobcoin"));
                    balances.put("gem", rs.getString("gem"));
                    data.put("balances", balances);
                    
                    Map<String, Long> lastSend = new HashMap<>();
                    lastSend.put("money", rs.getLong("money_last_send"));
                    lastSend.put("mobcoin", rs.getLong("mobcoin_last_send"));
                    lastSend.put("gem", rs.getLong("gem_last_send"));
                    data.put("last_send_time", lastSend);
                    
                    Map<String, Integer> sendCount = new HashMap<>();
                    sendCount.put("money", rs.getInt("money_send_count"));
                    sendCount.put("mobcoin", rs.getInt("mobcoin_send_count"));
                    sendCount.put("gem", rs.getInt("gem_send_count"));
                    data.put("daily_send_count", sendCount);
                    
                    Map<String, Long> resetTime = new HashMap<>();
                    resetTime.put("money", rs.getLong("money_reset_time"));
                    resetTime.put("mobcoin", rs.getLong("mobcoin_reset_time"));
                    resetTime.put("gem", rs.getLong("gem_reset_time"));
                    data.put("daily_reset_time", resetTime);
                    
                    return PlayerData.deserialize(data);
                }
                return null;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player data: " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO dzeconomy_players " +
                "(uuid, name, money, mobcoin, gem, money_last_send, mobcoin_last_send, gem_last_send, " +
                "money_send_count, mobcoin_send_count, gem_send_count, money_reset_time, mobcoin_reset_time, gem_reset_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "name=VALUES(name), money=VALUES(money), mobcoin=VALUES(mobcoin), gem=VALUES(gem), " +
                "money_last_send=VALUES(money_last_send), mobcoin_last_send=VALUES(mobcoin_last_send), " +
                "gem_last_send=VALUES(gem_last_send), money_send_count=VALUES(money_send_count), " +
                "mobcoin_send_count=VALUES(mobcoin_send_count), gem_send_count=VALUES(gem_send_count), " +
                "money_reset_time=VALUES(money_reset_time), mobcoin_reset_time=VALUES(mobcoin_reset_time), " +
                "gem_reset_time=VALUES(gem_reset_time)";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setString(1, data.getUUID().toString());
                stmt.setString(2, data.getName());
                stmt.setBigDecimal(3, data.getBalance(online.demonzdevelopment.currency.CurrencyType.MONEY));
                stmt.setBigDecimal(4, data.getBalance(online.demonzdevelopment.currency.CurrencyType.MOBCOIN));
                stmt.setBigDecimal(5, data.getBalance(online.demonzdevelopment.currency.CurrencyType.GEM));
                stmt.setLong(6, data.getLastSendTime(online.demonzdevelopment.currency.CurrencyType.MONEY));
                stmt.setLong(7, data.getLastSendTime(online.demonzdevelopment.currency.CurrencyType.MOBCOIN));
                stmt.setLong(8, data.getLastSendTime(online.demonzdevelopment.currency.CurrencyType.GEM));
                stmt.setInt(9, data.getDailySendCount(online.demonzdevelopment.currency.CurrencyType.MONEY));
                stmt.setInt(10, data.getDailySendCount(online.demonzdevelopment.currency.CurrencyType.MOBCOIN));
                stmt.setInt(11, data.getDailySendCount(online.demonzdevelopment.currency.CurrencyType.GEM));
                stmt.setLong(12, data.getDailyResetTime(online.demonzdevelopment.currency.CurrencyType.MONEY));
                stmt.setLong(13, data.getDailyResetTime(online.demonzdevelopment.currency.CurrencyType.MOBCOIN));
                stmt.setLong(14, data.getDailyResetTime(online.demonzdevelopment.currency.CurrencyType.GEM));
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player data: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT 1 FROM dzeconomy_players WHERE uuid = ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to check player existence: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<UUID> getUUIDByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT uuid FROM dzeconomy_players WHERE name = ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
                return null;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get UUID by name: " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}