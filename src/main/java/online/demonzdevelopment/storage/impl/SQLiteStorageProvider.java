package online.demonzdevelopment.storage.impl;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import online.demonzdevelopment.storage.StorageProvider;

import java.io.File;
import java.sql.*;
import java.util.UUID;

/**
 * SQLite storage implementation
 */
public class SQLiteStorageProvider implements StorageProvider {
    
    private final DZEconomy plugin;
    private Connection connection;
    
    public SQLiteStorageProvider(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "data/economy.db");
            dbFile.getParentFile().mkdirs();
            
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            
            createTables();
            plugin.getLogger().info("SQLite database initialized successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();
        
        // Players table
        stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "username VARCHAR(16)," +
                "first_join BIGINT," +
                "last_seen BIGINT," +
                "money_balance REAL," +
                "mobcoin_balance REAL," +
                "gem_balance REAL," +
                "money_sent BIGINT," +
                "mobcoin_sent BIGINT," +
                "gem_sent BIGINT," +
                "money_received BIGINT," +
                "mobcoin_received BIGINT," +
                "gem_received BIGINT" +
                ")");
        
        // Daily limits table
        stmt.execute("CREATE TABLE IF NOT EXISTS daily_limits (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "money_sends INT," +
                "mobcoin_sends INT," +
                "gem_sends INT," +
                "money_requests INT," +
                "mobcoin_requests INT," +
                "gem_requests INT," +
                "last_reset BIGINT," +
                "FOREIGN KEY (uuid) REFERENCES players(uuid)" +
                ")");
        
        // Cooldowns table
        stmt.execute("CREATE TABLE IF NOT EXISTS cooldowns (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "money_send_cooldown BIGINT," +
                "mobcoin_send_cooldown BIGINT," +
                "gem_send_cooldown BIGINT," +
                "money_request_cooldown BIGINT," +
                "mobcoin_request_cooldown BIGINT," +
                "gem_request_cooldown BIGINT," +
                "FOREIGN KEY (uuid) REFERENCES players(uuid)" +
                ")");
        
        stmt.close();
    }
    
    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM players WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                rs.close();
                stmt.close();
                return null;
            }
            
            PlayerData data = new PlayerData(uuid);
            data.setUsername(rs.getString("username"));
            data.setFirstJoin(rs.getLong("first_join"));
            data.setLastSeen(rs.getLong("last_seen"));
            
            // Load balances
            data.setBalance(CurrencyType.MONEY, rs.getDouble("money_balance"));
            data.setBalance(CurrencyType.MOBCOIN, rs.getDouble("mobcoin_balance"));
            data.setBalance(CurrencyType.GEM, rs.getDouble("gem_balance"));
            
            // Load statistics
            data.getMoneySent().put(CurrencyType.MONEY, rs.getLong("money_sent"));
            data.getMoneySent().put(CurrencyType.MOBCOIN, rs.getLong("mobcoin_sent"));
            data.getMoneySent().put(CurrencyType.GEM, rs.getLong("gem_sent"));
            data.getMoneyReceived().put(CurrencyType.MONEY, rs.getLong("money_received"));
            data.getMoneyReceived().put(CurrencyType.MOBCOIN, rs.getLong("mobcoin_received"));
            data.getMoneyReceived().put(CurrencyType.GEM, rs.getLong("gem_received"));
            
            rs.close();
            stmt.close();
            
            // Load daily limits
            loadDailyLimits(data);
            
            // Load cooldowns
            loadCooldowns(data);
            
            return data;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player data: " + e.getMessage());
            return null;
        }
    }
    
    private void loadDailyLimits(PlayerData data) throws SQLException {
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
    
    private void loadCooldowns(PlayerData data) throws SQLException {
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
        try {
            // Save main player data
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT OR REPLACE INTO players VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
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
            
            // Save daily limits
            saveDailyLimits(data);
            
            // Save cooldowns
            saveCooldowns(data);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save player data: " + e.getMessage());
        }
    }
    
    private void saveDailyLimits(PlayerData data) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO daily_limits VALUES (?,?,?,?,?,?,?,?)");
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
    
    private void saveCooldowns(PlayerData data) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO cooldowns VALUES (?,?,?,?,?,?,?)");
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
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT uuid FROM players WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            boolean exists = rs.next();
            rs.close();
            stmt.close();
            return exists;
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public void deletePlayerData(UUID uuid) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM players WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
            stmt.close();
            
            stmt = connection.prepareStatement("DELETE FROM daily_limits WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
            stmt.close();
            
            stmt = connection.prepareStatement("DELETE FROM cooldowns WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete player data: " + e.getMessage());
        }
    }
    
    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close SQLite connection: " + e.getMessage());
        }
    }
}