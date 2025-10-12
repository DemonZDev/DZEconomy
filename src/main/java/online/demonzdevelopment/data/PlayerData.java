package online.demonzdevelopment.data;

import online.demonzdevelopment.currency.CurrencyType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {
    private final UUID uuid;
    private String name;
    private final Map<CurrencyType, BigDecimal> balances;
    private final Map<CurrencyType, Long> lastSendTime;
    private final Map<CurrencyType, Integer> dailySendCount;
    private final Map<CurrencyType, Long> dailyResetTime;
    private boolean dirty;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.balances = new ConcurrentHashMap<>();
        this.lastSendTime = new ConcurrentHashMap<>();
        this.dailySendCount = new ConcurrentHashMap<>();
        this.dailyResetTime = new ConcurrentHashMap<>();
        this.dirty = false;
        
        for (CurrencyType type : CurrencyType.values()) {
            balances.put(type, BigDecimal.ZERO);
            lastSendTime.put(type, 0L);
            dailySendCount.put(type, 0);
            dailyResetTime.put(type, System.currentTimeMillis());
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
    }

    public synchronized BigDecimal getBalance(CurrencyType type) {
        return balances.getOrDefault(type, BigDecimal.ZERO);
    }

    public synchronized void setBalance(CurrencyType type, BigDecimal amount) {
        balances.put(type, amount);
        this.dirty = true;
    }

    public synchronized void addBalance(CurrencyType type, BigDecimal amount) {
        BigDecimal current = getBalance(type);
        setBalance(type, current.add(amount));
    }

    public synchronized void subtractBalance(CurrencyType type, BigDecimal amount) {
        BigDecimal current = getBalance(type);
        setBalance(type, current.subtract(amount));
    }

    public synchronized boolean hasBalance(CurrencyType type, BigDecimal amount) {
        return getBalance(type).compareTo(amount) >= 0;
    }

    public long getLastSendTime(CurrencyType type) {
        return lastSendTime.getOrDefault(type, 0L);
    }

    public void setLastSendTime(CurrencyType type, long time) {
        lastSendTime.put(type, time);
        this.dirty = true;
    }

    public int getDailySendCount(CurrencyType type) {
        return dailySendCount.getOrDefault(type, 0);
    }

    public void incrementDailySendCount(CurrencyType type) {
        int current = getDailySendCount(type);
        dailySendCount.put(type, current + 1);
        this.dirty = true;
    }

    public void resetDailySendCount(CurrencyType type) {
        dailySendCount.put(type, 0);
        dailyResetTime.put(type, System.currentTimeMillis());
        this.dirty = true;
    }

    public long getDailyResetTime(CurrencyType type) {
        return dailyResetTime.getOrDefault(type, System.currentTimeMillis());
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("uuid", uuid.toString());
        data.put("name", name);
        
        Map<String, String> balanceMap = new HashMap<>();
        for (Map.Entry<CurrencyType, BigDecimal> entry : balances.entrySet()) {
            balanceMap.put(entry.getKey().getKey(), entry.getValue().toPlainString());
        }
        data.put("balances", balanceMap);
        
        Map<String, Long> sendTimeMap = new HashMap<>();
        for (Map.Entry<CurrencyType, Long> entry : lastSendTime.entrySet()) {
            sendTimeMap.put(entry.getKey().getKey(), entry.getValue());
        }
        data.put("last_send_time", sendTimeMap);
        
        Map<String, Integer> sendCountMap = new HashMap<>();
        for (Map.Entry<CurrencyType, Integer> entry : dailySendCount.entrySet()) {
            sendCountMap.put(entry.getKey().getKey(), entry.getValue());
        }
        data.put("daily_send_count", sendCountMap);
        
        Map<String, Long> resetTimeMap = new HashMap<>();
        for (Map.Entry<CurrencyType, Long> entry : dailyResetTime.entrySet()) {
            resetTimeMap.put(entry.getKey().getKey(), entry.getValue());
        }
        data.put("daily_reset_time", resetTimeMap);
        
        return data;
    }

    @SuppressWarnings("unchecked")
    public static PlayerData deserialize(Map<String, Object> data) {
        UUID uuid = UUID.fromString((String) data.get("uuid"));
        String name = (String) data.get("name");
        
        PlayerData playerData = new PlayerData(uuid, name);
        
        if (data.containsKey("balances")) {
            Map<String, Object> balanceMap = (Map<String, Object>) data.get("balances");
            for (Map.Entry<String, Object> entry : balanceMap.entrySet()) {
                CurrencyType type = CurrencyType.fromString(entry.getKey());
                if (type != null) {
                    BigDecimal amount = new BigDecimal(entry.getValue().toString());
                    playerData.setBalance(type, amount);
                }
            }
        }
        
        if (data.containsKey("last_send_time")) {
            Map<String, Object> sendTimeMap = (Map<String, Object>) data.get("last_send_time");
            for (Map.Entry<String, Object> entry : sendTimeMap.entrySet()) {
                CurrencyType type = CurrencyType.fromString(entry.getKey());
                if (type != null) {
                    long time = ((Number) entry.getValue()).longValue();
                    playerData.setLastSendTime(type, time);
                }
            }
        }
        
        if (data.containsKey("daily_send_count")) {
            Map<String, Object> sendCountMap = (Map<String, Object>) data.get("daily_send_count");
            for (Map.Entry<String, Object> entry : sendCountMap.entrySet()) {
                CurrencyType type = CurrencyType.fromString(entry.getKey());
                if (type != null) {
                    int count = ((Number) entry.getValue()).intValue();
                    playerData.dailySendCount.put(type, count);
                }
            }
        }
        
        if (data.containsKey("daily_reset_time")) {
            Map<String, Object> resetTimeMap = (Map<String, Object>) data.get("daily_reset_time");
            for (Map.Entry<String, Object> entry : resetTimeMap.entrySet()) {
                CurrencyType type = CurrencyType.fromString(entry.getKey());
                if (type != null) {
                    long time = ((Number) entry.getValue()).longValue();
                    playerData.dailyResetTime.put(type, time);
                }
            }
        }
        
        playerData.setDirty(false);
        return playerData;
    }
}