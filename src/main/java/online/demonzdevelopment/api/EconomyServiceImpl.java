package online.demonzdevelopment.api;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import online.demonzdevelopment.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyServiceImpl implements EconomyService {
    private final DZEconomy plugin;

    public EconomyServiceImpl(DZEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public BigDecimal getBalance(UUID player, CurrencyType currency) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return data != null ? data.getBalance(currency) : BigDecimal.ZERO;
    }

    @Override
    public CompletableFuture<BigDecimal> getBalanceAsync(UUID player, CurrencyType currency) {
        return CompletableFuture.supplyAsync(() -> getBalance(player, currency));
    }

    @Override
    public boolean hasBalance(UUID player, CurrencyType currency, BigDecimal amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return data != null && data.hasBalance(currency, amount);
    }

    @Override
    public CompletableFuture<Boolean> hasBalanceAsync(UUID player, CurrencyType currency, BigDecimal amount) {
        return CompletableFuture.supplyAsync(() -> hasBalance(player, currency, amount));
    }

    @Override
    public TransactionResult deposit(UUID player, CurrencyType currency, BigDecimal amount, String source) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            return new TransactionResult(false, "Player data not found", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        BigDecimal preBalance = data.getBalance(currency);
        data.addBalance(currency, amount);
        BigDecimal postBalance = data.getBalance(currency);
        
        return new TransactionResult(true, "Deposit successful", preBalance, postBalance, BigDecimal.ZERO);
    }

    @Override
    public CompletableFuture<TransactionResult> depositAsync(UUID player, CurrencyType currency, BigDecimal amount, String source) {
        return CompletableFuture.supplyAsync(() -> deposit(player, currency, amount, source));
    }

    @Override
    public TransactionResult withdraw(UUID player, CurrencyType currency, BigDecimal amount, String reason) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            return new TransactionResult(false, "Player data not found", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        if (!data.hasBalance(currency, amount)) {
            return new TransactionResult(false, "Insufficient funds", data.getBalance(currency), data.getBalance(currency), BigDecimal.ZERO);
        }
        
        BigDecimal preBalance = data.getBalance(currency);
        data.subtractBalance(currency, amount);
        BigDecimal postBalance = data.getBalance(currency);
        
        return new TransactionResult(true, "Withdrawal successful", preBalance, postBalance, BigDecimal.ZERO);
    }

    @Override
    public CompletableFuture<TransactionResult> withdrawAsync(UUID player, CurrencyType currency, BigDecimal amount, String reason) {
        return CompletableFuture.supplyAsync(() -> withdraw(player, currency, amount, reason));
    }

    @Override
    public TransactionResult transfer(UUID from, UUID to, CurrencyType currency, BigDecimal amount, boolean applyTax) {
        PlayerData fromData = plugin.getPlayerDataManager().getPlayerData(from);
        PlayerData toData = plugin.getPlayerDataManager().getPlayerData(to);
        
        if (fromData == null || toData == null) {
            return new TransactionResult(false, "Player data not found", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        BigDecimal tax = BigDecimal.ZERO;
        if (applyTax) {
            Player player = Bukkit.getPlayer(from);
            if (player != null) {
                tax = plugin.getEconomyManager().calculateTax(player, currency, amount);
            }
        }
        
        BigDecimal total = amount.add(tax);
        
        if (!fromData.hasBalance(currency, total)) {
            return new TransactionResult(false, "Insufficient funds", fromData.getBalance(currency), fromData.getBalance(currency), tax);
        }
        
        BigDecimal preBalance = fromData.getBalance(currency);
        fromData.subtractBalance(currency, total);
        toData.addBalance(currency, amount);
        BigDecimal postBalance = fromData.getBalance(currency);
        
        return new TransactionResult(true, "Transfer successful", preBalance, postBalance, tax);
    }

    @Override
    public CompletableFuture<TransactionResult> transferAsync(UUID from, UUID to, CurrencyType currency, BigDecimal amount, boolean applyTax) {
        return CompletableFuture.supplyAsync(() -> transfer(from, to, currency, amount, applyTax));
    }

    @Override
    public ConversionResult convert(UUID player, CurrencyType from, CurrencyType to, BigDecimal amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            return new ConversionResult(false, "Player data not found", from, to, amount, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        Player onlinePlayer = Bukkit.getPlayer(player);
        BigDecimal tax = BigDecimal.ZERO;
        if (onlinePlayer != null) {
            tax = plugin.getEconomyManager().calculateConversionTax(onlinePlayer, amount);
        }
        
        BigDecimal total = amount.add(tax);
        
        if (!data.hasBalance(from, total)) {
            return new ConversionResult(false, "Insufficient funds", from, to, amount, BigDecimal.ZERO, tax);
        }
        
        BigDecimal converted = plugin.getEconomyManager().convert(from, to, amount);
        
        data.subtractBalance(from, total);
        data.addBalance(to, converted);
        
        return new ConversionResult(true, "Conversion successful", from, to, amount, converted, tax);
    }

    @Override
    public CompletableFuture<ConversionResult> convertAsync(UUID player, CurrencyType from, CurrencyType to, BigDecimal amount) {
        return CompletableFuture.supplyAsync(() -> convert(player, from, to, amount));
    }
}