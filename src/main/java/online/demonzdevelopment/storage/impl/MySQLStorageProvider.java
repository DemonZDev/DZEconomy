package online.demonzdevelopment.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.storage.StorageProvider;

import java.sql.*;
import java.util.UUID;

/**
 * MySQL storage implementation with HikariCP connection pooling
 */
public class MySQLStorageProvider implements StorageProvider {
    
    private final DZEconomy plugin;
    private HikariDataSource dataSource;
    
    public MySQLStorageProvider(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void initialize() {
        try {
            HikariConfig config = new HikariConfig();
            
            String host = plugin.getConfigManager().getConfig().getString("storage.mysql.host");
            int port = plugin.getConfigManager().getConfig().getInt("storage.mysql.port");
            String database = plugin.getConfigManager().getConfig().getString("storage.mysql.database");
            String username = plugin.getConfigManager().getConfig().getString("storage.mysql.username");
            String password = plugin.getConfigManager().getConfig().getString("storage.mysql.password");
            
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            
            config.setMaximumPoolSize(plugin.getConfigManager().getConfig().getInt("storage.mysql.pool.maximum-pool-size", 10));
            config.setMinimumIdle(plugin.getConfigManager().getConfig().getInt("storage.mysql.pool.minimum-idle", 2));
            config.setConnectionTimeout(plugin.getConfigManager().getConfig().getLong("storage.mysql.pool.connection-timeout", 30000));
            config.setIdleTimeout(plugin.getConfigManager().getConfig().getLong("storage.mysql.pool.idle-timeout", 600000));
            config.setMaxLifetime(plugin.getConfigManager().getConfig().getLong("storage.mysql.pool.max-lifetime", 1800000));
            
            dataSource = new HikariDataSource(config);
            
            createTables();
            plugin.getLogger().info("MySQL database initialized successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MySQL database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createTables() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "username VARCHAR(16)," +
                    "first_join BIGINT," +
                    "last_seen BIGINT," +
                    "money_balance DOUBLE," +
                    "mobcoin_balance DOUBLE," +
                    "gem_balance DOUBLE," +
                    "money_sent BIGINT," +
                    "mobcoin_sent BIGINT," +
                    "gem_sent BIGINT," +
                    "money_received BIGINT," +
                    "mobcoin_received BIGINT," +
                    "gem_received BIGINT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS daily_limits (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "money_sends INT," +
                    "mobcoin_sends INT," +
                    "gem_sends INT," +
                    "money_requests INT," +
                    "mobcoin_requests INT," +
                    "gem_requests INT," +
                    "last_reset BIGINT," +
                    "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS cooldowns (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "money_send_cooldown BIGINT," +
                    "mobcoin_send_cooldown BIGINT," +
                    "gem_send_cooldown BIGINT," +
                    "money_request_cooldown BIGINT," +
                    "mobcoin_request_cooldown BIGINT," +
                    "gem_request_cooldown BIGINT," +
                    "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }
    
    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM players WHERE uuid = ?")) {

            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                PlayerData data = new PlayerData(uuid);
                data.setUsername(rs.getString("username"));
                data.setFirstJoin(rs.getLong("first_join"));
                data.setLastSeen(rs.getLong("last_seen"));

                data.setBalance(CurrencyType.MONEY, rs.getDouble("money_balance"));
                data.setBalance(CurrencyType.MOBCOIN, rs.getDouble("mobcoin_balance"));
                data.setBalance(CurrencyType.GEM, rs.getDouble("gem_balance"));

                data.getMoneySent().put(CurrencyType.MONEY, rs.getLong("money_sent"));
                data.getMoneySent().put(CurrencyType.MOBCOIN, rs.getLong("mobcoin_sent"));
                data.getMoneySent().put(CurrencyType.GEM, rs.getLong("gem_sent"));
                data.getMoneyReceived().put(CurrencyType.MONEY, rs.getLong("money_received"));
                data.getMoneyReceived().put(CurrencyType.MOBCOIN, rs.getLong("mobcoin_received"));
                data.getMoneyReceived().put(CurrencyType.GEM, rs.getLong("gem_received"));

                loadDailyLimits(connection, data);
                loadCooldowns(connection, data);

                return data;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player data: " + e.getMessage());
            return null;
        }
    }
    
    private void loadDailyLimits(Connection connection, PlayerData data) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM daily_limits WHERE uuid = ?");
        stmt.setString(1, data.getUUID().toString());
        
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            data.getDailySendCounts().put(CurrencyType.MONEY, rs.getInt("money_sends"));
            data.getDailySendCounts().put(CurrencyType.MOBCOIN, rs.getInt("mobcoin_sends"));
            data.getDailySendCounts().put(CurrencyType.GEM, rs.getInt("gem_sends"));
            data.getDailyRequestCounts().put(CurrencyType.MONEY, rs.getInt("money_requests"));
            data.getDailyRequestCounts().put(CurrencyType.MOBCOIN, rs.getInt("mobcoin_requests"));
            data.getDailyRequestCounts().put(CurrencyType.GEM, rs.getInt("gem_requests"));
            data.setLastDailyReset(rs.getLong("last_reset"));
        }
        
        rs.close();
        stmt.close();
    }
    
    private void loadCooldowns(Connection connection, PlayerData data) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM cooldowns WHERE uuid = ?");
        stmt.setString(1, data.getUUID().toString());
        
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            data.getSendCooldowns().put(CurrencyType.MONEY, rs.getLong("money_send_cooldown"));
            data.getSendCooldowns().put(CurrencyType.MOBCOIN, rs.getLong("mobcoin_send_cooldown"));
            data.getSendCooldowns().put(CurrencyType.GEM, rs.getLong("gem_send_cooldown"));
            data.getRequestCooldowns().put(CurrencyType.MONEY, rs.getLong("money_request_cooldown"));
            data.getRequestCooldowns().put(CurrencyType.MOBCOIN, rs.getLong("mobcoin_request_cooldown"));
            data.getRequestCooldowns().put(CurrencyType.GEM, rs.getLong("gem_request_cooldown"));
        }
        
        rs.close();
        stmt.close();
    }
    
    @Override
    public void savePlayerData(PlayerData data) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO players VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "username=VALUES(username), last_seen=VALUES(last_seen), " +
                    "money_balance=VALUES(money_balance), mobcoin_balance=VALUES(mobcoin_balance), " +
                    "gem_balance=VALUES(gem_balance), money_sent=VALUES(money_sent), " +
                    "mobcoin_sent=VALUES(mobcoin_sent), gem_sent=VALUES(gem_sent), " +
                    "money_received=VALUES(money_received), mobcoin_received=VALUES(mobcoin_received), " +
                    "gem_received=VALUES(gem_received)");
            
            stmt.setString(1, data.getUUID().toString());
            stmt.setString(2, data.getUsername());
            stmt.setLong(3, data.getFirstJoin());
            stmt.setLong(4, data.getLastSeen());
            stmt.setDouble(5, data.getBalance(CurrencyType.MONEY));
            stmt.setDouble(6, data.getBalance(CurrencyType.MOBCOIN));
            stmt.setDouble(7, data.getBalance(CurrencyType.GEM));
            stmt.setLong(8, data.getMoneySent().get(CurrencyType.MONEY));
            stmt.setLong(9, data.getMoneySent().get(CurrencyType.MOBCOIN));
            stmt.setLong(10, data.getMoneySent().get(CurrencyType.GEM));
            stmt.setLong(11, data.getMoneyReceived().get(CurrencyType.MONEY));
            stmt.setLong(12, data.getMoneyReceived().get(CurrencyType.MOBCOIN));
            stmt.setLong(13, data.getMoneyReceived().get(CurrencyType.GEM));
            stmt.executeUpdate();
            stmt.close();
            
            saveDailyLimits(connection, data);
            saveCooldowns(connection, data);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save player data: " + e.getMessage());
        }
    }
    
    private void saveDailyLimits(Connection connection, PlayerData data) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO daily_limits VALUES (?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE " +
                "money_sends=VALUES(money_sends), mobcoin_sends=VALUES(mobcoin_sends), " +
                "gem_sends=VALUES(gem_sends), money_requests=VALUES(money_requests), " +
                "mobcoin_requests=VALUES(mobcoin_requests), gem_requests=VALUES(gem_requests), " +
                "last_reset=VALUES(last_reset)");
        
        stmt.setString(1, data.getUUID().toString());
        stmt.setInt(2, data.getDailySendCounts().get(CurrencyType.MONEY));
        stmt.setInt(3, data.getDailySendCounts().get(CurrencyType.MOBCOIN));
        stmt.setInt(4, data.getDailySendCounts().get(CurrencyType.GEM));
        stmt.setInt(5, data.getDailyRequestCounts().get(CurrencyType.MONEY));
        stmt.setInt(6, data.getDailyRequestCounts().get(CurrencyType.MOBCOIN));
        stmt.setInt(7, data.getDailyRequestCounts().get(CurrencyType.GEM));
        stmt.setLong(8, data.getLastDailyReset());
        stmt.executeUpdate();
        stmt.close();
    }
    
    private void saveCooldowns(Connection connection, PlayerData data) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO cooldowns VALUES (?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE " +
                "money_send_cooldown=VALUES(money_send_cooldown), " +
                "mobcoin_send_cooldown=VALUES(mobcoin_send_cooldown), " +
                "gem_send_cooldown=VALUES(gem_send_cooldown), " +
                "money_request_cooldown=VALUES(money_request_cooldown), " +
                "mobcoin_request_cooldown=VALUES(mobcoin_request_cooldown), " +
                "gem_request_cooldown=VALUES(gem_request_cooldown)");
        
        stmt.setString(1, data.getUUID().toString());
        stmt.setLong(2, data.getSendCooldowns().get(CurrencyType.MONEY));
        stmt.setLong(3, data.getSendCooldowns().get(CurrencyType.MOBCOIN));
        stmt.setLong(4, data.getSendCooldowns().get(CurrencyType.GEM));
        stmt.setLong(5, data.getRequestCooldowns().get(CurrencyType.MONEY));
        stmt.setLong(6, data.getRequestCooldowns().get(CurrencyType.MOBCOIN));
        stmt.setLong(7, data.getRequestCooldowns().get(CurrencyType.GEM));
        stmt.executeUpdate();
        stmt.close();
    }
    
    @Override
    public boolean playerDataExists(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT uuid FROM players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            boolean exists = rs.next();
            rs.close();
            return exists;
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public void deletePlayerData(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("DELETE FROM players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete player data: " + e.getMessage());
        }
    }
    
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}