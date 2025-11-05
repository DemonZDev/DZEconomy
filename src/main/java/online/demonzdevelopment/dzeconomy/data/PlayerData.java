package online.demonzdevelopment.dzeconomy.data;

import online.demonzdevelopment.dzeconomy.currency.CurrencyType;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents all economy data for a single player
 */
public class PlayerData {
    
    private final UUID uuid;
    private String username;
    private long firstJoin;
    private long lastSeen;
    
    // Balances for each currency
    private final Map<CurrencyType, Double> balances;
    
    // Statistics
    private final Map<CurrencyType, Long> moneySent;
    private final Map<CurrencyType, Long> moneyReceived;
    
    // Daily limits tracking
    private final Map<CurrencyType, Integer> dailySendCount;
    private final Map<CurrencyType, Integer> dailyRequestCount;
    private long lastDailyReset;
    
    // Cooldowns (timestamps)
    private final Map<CurrencyType, Long> sendCooldowns;
    private final Map<CurrencyType, Long> requestCooldowns;
    
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.username = Bukkit.getOfflinePlayer(uuid).getName();
        this.balances = new HashMap<>();
        this.moneySent = new HashMap<>();
        this.moneyReceived = new HashMap<>();
        this.dailySendCount = new HashMap<>();
        this.dailyRequestCount = new HashMap<>();
        this.sendCooldowns = new HashMap<>();
        this.requestCooldowns = new HashMap<>();
        
        // Initialize all currencies to 0
        for (CurrencyType type : CurrencyType.values()) {
            balances.put(type, 0.0);
            moneySent.put(type, 0L);
            moneyReceived.put(type, 0L);
            dailySendCount.put(type, 0);
            dailyRequestCount.put(type, 0);
            sendCooldowns.put(type, 0L);
            requestCooldowns.put(type, 0L);
        }
    }
    
    // Balance methods
    
    public double getBalance(CurrencyType currency) {
        return balances.getOrDefault(currency, 0.0);
    }
    
    public void setBalance(CurrencyType currency, double amount) {
        balances.put(currency, amount);
    }
    
    public void addBalance(CurrencyType currency, double amount) {
        balances.put(currency, getBalance(currency) + amount);
    }
    
    public void removeBalance(CurrencyType currency, double amount) {
        balances.put(currency, getBalance(currency) - amount);
    }
    
    // Statistics
    
    public void addMoneySent(CurrencyType currency, double amount) {
        long current = moneySent.getOrDefault(currency, 0L);
        moneySent.put(currency, current + (long) amount);
    }
    
    public void addMoneyReceived(CurrencyType currency, double amount) {
        long current = moneyReceived.getOrDefault(currency, 0L);
        moneyReceived.put(currency, current + (long) amount);
    }
    
    // Daily limits
    
    public int getDailySendCount(CurrencyType currency) {
        return dailySendCount.getOrDefault(currency, 0);
    }
    
    public void incrementDailySendCount(CurrencyType currency) {
        dailySendCount.put(currency, getDailySendCount(currency) + 1);
    }
    
    public int getDailyRequestCount(CurrencyType currency) {
        return dailyRequestCount.getOrDefault(currency, 0);
    }
    
    public void incrementDailyRequestCount(CurrencyType currency) {
        dailyRequestCount.put(currency, getDailyRequestCount(currency) + 1);
    }
    
    public void resetDailyLimits() {
        for (CurrencyType type : CurrencyType.values()) {
            dailySendCount.put(type, 0);
            dailyRequestCount.put(type, 0);
        }
        lastDailyReset = System.currentTimeMillis();
    }
    
    // Cooldowns
    
    public long getSendCooldown(CurrencyType currency) {
        return sendCooldowns.getOrDefault(currency, 0L);
    }
    
    public void setSendCooldown(CurrencyType currency, long timestamp) {
        sendCooldowns.put(currency, timestamp);
    }
    
    public long getRequestCooldown(CurrencyType currency) {
        return requestCooldowns.getOrDefault(currency, 0L);
    }
    
    public void setRequestCooldown(CurrencyType currency, long timestamp) {
        requestCooldowns.put(currency, timestamp);
    }
    
    public boolean isSendCooldownActive(CurrencyType currency, int cooldownSeconds) {
        long cooldownEnd = getSendCooldown(currency) + (cooldownSeconds * 1000L);
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    public long getSendCooldownRemaining(CurrencyType currency, int cooldownSeconds) {
        long cooldownEnd = getSendCooldown(currency) + (cooldownSeconds * 1000L);
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
    
    public boolean isRequestCooldownActive(CurrencyType currency, int cooldownSeconds) {
        long cooldownEnd = getRequestCooldown(currency) + (cooldownSeconds * 1000L);
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    public long getRequestCooldownRemaining(CurrencyType currency, int cooldownSeconds) {
        long cooldownEnd = getRequestCooldown(currency) + (cooldownSeconds * 1000L);
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
    
    // Getters and setters
    
    public UUID getUUID() {
        return uuid;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public long getFirstJoin() {
        return firstJoin;
    }
    
    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public long getLastDailyReset() {
        return lastDailyReset;
    }
    
    public void setLastDailyReset(long lastDailyReset) {
        this.lastDailyReset = lastDailyReset;
    }
    
    public Map<CurrencyType, Double> getBalances() {
        return balances;
    }
    
    public Map<CurrencyType, Long> getMoneySent() {
        return moneySent;
    }
    
    public Map<CurrencyType, Long> getMoneyReceived() {
        return moneyReceived;
    }
    
    public Map<CurrencyType, Integer> getDailySendCounts() {
        return dailySendCount;
    }
    
    public Map<CurrencyType, Integer> getDailyRequestCounts() {
        return dailyRequestCount;
    }
    
    public Map<CurrencyType, Long> getSendCooldowns() {
        return sendCooldowns;
    }
    
    public Map<CurrencyType, Long> getRequestCooldowns() {
        return requestCooldowns;
    }
}