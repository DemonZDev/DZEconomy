package online.demonzdevelopment.dzeconomy.data;

import online.demonzdevelopment.dzeconomy.currency.CurrencyType;

import java.util.UUID;

/**
 * Represents a currency transfer request between two players
 */
public class CurrencyRequest {
    
    private final UUID requesterUUID;
    private final UUID requestedPlayerUUID;
    private final CurrencyType currency;
    private final double amount;
    private final long timestamp;
    
    public CurrencyRequest(UUID requesterUUID, UUID requestedPlayerUUID, CurrencyType currency, double amount) {
        this.requesterUUID = requesterUUID;
        this.requestedPlayerUUID = requestedPlayerUUID;
        this.currency = currency;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Check if the request has expired
     */
    public boolean isExpired(int timeoutSeconds) {
        long currentTime = System.currentTimeMillis();
        long expirationTime = timestamp + (timeoutSeconds * 1000L);
        return currentTime >= expirationTime;
    }
    
    /**
     * Get remaining time in seconds
     */
    public long getRemainingTime(int timeoutSeconds) {
        long currentTime = System.currentTimeMillis();
        long expirationTime = timestamp + (timeoutSeconds * 1000L);
        long remaining = expirationTime - currentTime;
        return Math.max(0, remaining / 1000);
    }
    
    // Getters
    
    public UUID getRequesterUUID() {
        return requesterUUID;
    }
    
    public UUID getRequestedPlayerUUID() {
        return requestedPlayerUUID;
    }
    
    public CurrencyType getCurrency() {
        return currency;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}