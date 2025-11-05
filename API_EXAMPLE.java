package com.example.myplugin;

// Example for DZEconomy v1.2.0 by DemonZ Development

import online.demonzdevelopment.dzeconomy.api.DZEconomyAPI;
import online.demonzdevelopment.dzeconomy.currency.CurrencyType;
import online.demonzdevelopment.dzeconomy.rank.Rank;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Example plugin demonstrating DZEconomy API usage
 * 
 * IMPORTANT: Add DZEconomy as a dependency in your plugin.yml:
 * depend: [DZEconomy]
 * or
 * softdepend: [DZEconomy]
 */
public class ExamplePlugin extends JavaPlugin {
    
    private DZEconomyAPI economyAPI;
    
    @Override
    public void onEnable() {
        // Check if DZEconomy is loaded
        if (Bukkit.getPluginManager().getPlugin("DZEconomy") == null) {
            getLogger().severe("DZEconomy not found! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get DZEconomy API instance via ServicesManager
        RegisteredServiceProvider<DZEconomyAPI> provider = Bukkit.getServicesManager()
                .getRegistration(DZEconomyAPI.class);
        
        if (provider == null) {
            getLogger().severe("DZEconomy API not registered! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        economyAPI = provider.getProvider();
        getLogger().info("Successfully hooked into DZEconomy v1.2.0+!");
    }
    
    /**
     * Example: Check and deduct money from player
     */
    public boolean purchaseItem(Player player, double cost) {
        UUID uuid = player.getUniqueId();
        
        // Check if player has enough money
        if (!economyAPI.hasBalance(uuid, CurrencyType.MONEY, cost)) {
            player.sendMessage("§cYou need $" + cost + " to purchase this item!");
            return false;
        }
        
        // Deduct money
        economyAPI.removeCurrency(uuid, CurrencyType.MONEY, cost);
        player.sendMessage("§aPurchased! -$" + cost);
        return true;
    }
    
    /**
     * Example: Reward player with MobCoins
     */
    public void rewardPlayer(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        
        economyAPI.addCurrency(uuid, CurrencyType.MOBCOIN, amount);
        player.sendMessage("§6+§e" + amount + " MC §6rewarded!");
    }
    
    /**
     * Example: Transfer currency between players
     */
    public boolean transferCurrency(Player from, Player to, double amount) {
        UUID fromUUID = from.getUniqueId();
        UUID toUUID = to.getUniqueId();
        
        // Attempt transfer (returns false if sender doesn't have balance)
        if (economyAPI.transferCurrency(fromUUID, toUUID, CurrencyType.MONEY, amount)) {
            from.sendMessage("§aTransferred $" + amount + " to " + to.getName());
            to.sendMessage("§aReceived $" + amount + " from " + from.getName());
            return true;
        } else {
            from.sendMessage("§cInsufficient funds!");
            return false;
        }
    }
    
    /**
     * Example: Convert player's money to gems
     */
    public boolean convertToGems(Player player, double moneyAmount) {
        UUID uuid = player.getUniqueId();
        
        // Convert money to gems (automatically calculates conversion rate)
        if (economyAPI.convertCurrency(uuid, CurrencyType.MONEY, CurrencyType.GEM, moneyAmount)) {
            player.sendMessage("§aSuccessfully converted $" + moneyAmount + " to Gems!");
            return true;
        } else {
            player.sendMessage("§cConversion failed! Insufficient balance.");
            return false;
        }
    }
    
    /**
     * Example: Get player's rank information
     */
    public void displayRankInfo(Player player) {
        UUID uuid = player.getUniqueId();
        
        Rank rank = economyAPI.getPlayerRank(uuid);
        
        player.sendMessage("§7Your rank: " + rank.getDisplayName());
        player.sendMessage("§7Priority: §e" + rank.getPriority());
        
        // Get currency-specific settings
        Rank.RankCurrencySettings moneySettings = rank.getMoneySettings();
        player.sendMessage("§7Transfer tax: §e" + moneySettings.getTransferTax() + "%");
        player.sendMessage("§7Daily send limit: §e" + moneySettings.getDailyTransferLimit());
        player.sendMessage("§7Cooldown: §e" + moneySettings.getTransferCooldown() + "s");
    }
    
    /**
     * Example: Display formatted balances
     */
    public void displayBalances(Player player) {
        UUID uuid = player.getUniqueId();
        
        String money = economyAPI.formatCurrency(
                economyAPI.getBalance(uuid, CurrencyType.MONEY), 
                CurrencyType.MONEY);
        
        String mobcoin = economyAPI.formatCurrency(
                economyAPI.getBalance(uuid, CurrencyType.MOBCOIN), 
                CurrencyType.MOBCOIN);
        
        String gem = economyAPI.formatCurrency(
                economyAPI.getBalance(uuid, CurrencyType.GEM), 
                CurrencyType.GEM);
        
        player.sendMessage("§7Your Balances:");
        player.sendMessage("§a  Money: " + money);
        player.sendMessage("§6  MobCoin: " + mobcoin);
        player.sendMessage("§b  Gems: " + gem);
    }
    
    /**
     * Example: Check if player can afford something
     */
    public boolean canAfford(Player player, CurrencyType currency, double amount) {
        return economyAPI.hasBalance(player.getUniqueId(), currency, amount);
    }
    
    /**
     * Example: Give daily reward based on rank
     */
    public void giveDailyReward(Player player) {
        UUID uuid = player.getUniqueId();
        Rank rank = economyAPI.getPlayerRank(uuid);
        
        // Give reward based on rank priority
        double reward = 1000.0 * (1 + rank.getPriority() * 0.1);
        
        economyAPI.addCurrency(uuid, CurrencyType.MONEY, reward);
        player.sendMessage("§aDaily reward: " + economyAPI.formatCurrency(reward, CurrencyType.MONEY));
    }
}