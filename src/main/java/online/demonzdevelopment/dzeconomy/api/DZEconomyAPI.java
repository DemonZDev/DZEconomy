package online.demonzdevelopment.dzeconomy.api;

import online.demonzdevelopment.dzeconomy.currency.CurrencyType;
import online.demonzdevelopment.dzeconomy.rank.Rank;

import java.util.List;
import java.util.UUID;

/**
 * Public API for third-party plugins to interact with DZEconomy
 * 
 * Access via Bukkit's ServicesManager:
 * DZEconomyAPI api = Bukkit.getServicesManager().getRegistration(DZEconomyAPI.class).getProvider();
 */
public interface DZEconomyAPI {
    
    // Balance operations
    
    /**
     * Get a player's balance for a specific currency
     * 
     * @param player Player UUID
     * @param currency Currency type
     * @return Balance amount
     */
    double getBalance(UUID player, CurrencyType currency);
    
    /**
     * Check if player has at least the specified amount
     * 
     * @param player Player UUID
     * @param currency Currency type
     * @param amount Amount to check
     * @return True if player has enough balance
     */
    boolean hasBalance(UUID player, CurrencyType currency, double amount);
    
    /**
     * Add currency to a player's balance
     * 
     * @param player Player UUID
     * @param currency Currency type
     * @param amount Amount to add
     */
    void addCurrency(UUID player, CurrencyType currency, double amount);
    
    /**
     * Remove currency from a player's balance
     * 
     * @param player Player UUID
     * @param currency Currency type
     * @param amount Amount to remove
     */
    void removeCurrency(UUID player, CurrencyType currency, double amount);
    
    /**
     * Set a player's balance to a specific amount
     * 
     * @param player Player UUID
     * @param currency Currency type
     * @param amount New balance amount
     */
    void setCurrency(UUID player, CurrencyType currency, double amount);
    
    // Transaction operations
    
    /**
     * Transfer currency from one player to another (no tax)
     * 
     * @param from Sender UUID
     * @param to Receiver UUID
     * @param currency Currency type
     * @param amount Amount to transfer
     * @return True if transfer was successful
     */
    boolean transferCurrency(UUID from, UUID to, CurrencyType currency, double amount);
    
    /**
     * Convert currency for a player
     * 
     * @param player Player UUID
     * @param from Source currency
     * @param to Target currency
     * @param amount Amount to convert (in source currency)
     * @return True if conversion was successful
     */
    boolean convertCurrency(UUID player, CurrencyType from, CurrencyType to, double amount);
    
    // Rank operations
    
    /**
     * Get a player's current rank
     * 
     * @param player Player UUID
     * @return Player's rank
     */
    Rank getPlayerRank(UUID player);
    
    /**
     * Get all available ranks
     * 
     * @return List of all ranks
     */
    List<Rank> getAllRanks();
    
    // Formatting utilities
    
    /**
     * Format a currency amount with symbol
     * 
     * @param amount Amount to format
     * @param currency Currency type
     * @return Formatted string (e.g., "$1.5K")
     */
    String formatCurrency(double amount, CurrencyType currency);
    
    /**
     * Format a currency amount in short form
     * 
     * @param amount Amount to format
     * @param currency Currency type
     * @return Short form string (e.g., "1.5K")
     */
    String formatCurrencyShort(double amount, CurrencyType currency);
}