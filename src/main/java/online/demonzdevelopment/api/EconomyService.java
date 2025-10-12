package online.demonzdevelopment.api;

import online.demonzdevelopment.currency.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {
    BigDecimal getBalance(UUID player, CurrencyType currency);
    
    CompletableFuture<BigDecimal> getBalanceAsync(UUID player, CurrencyType currency);
    
    boolean hasBalance(UUID player, CurrencyType currency, BigDecimal amount);
    
    CompletableFuture<Boolean> hasBalanceAsync(UUID player, CurrencyType currency, BigDecimal amount);
    
    TransactionResult deposit(UUID player, CurrencyType currency, BigDecimal amount, String source);
    
    CompletableFuture<TransactionResult> depositAsync(UUID player, CurrencyType currency, BigDecimal amount, String source);
    
    TransactionResult withdraw(UUID player, CurrencyType currency, BigDecimal amount, String reason);
    
    CompletableFuture<TransactionResult> withdrawAsync(UUID player, CurrencyType currency, BigDecimal amount, String reason);
    
    TransactionResult transfer(UUID from, UUID to, CurrencyType currency, BigDecimal amount, boolean applyTax);
    
    CompletableFuture<TransactionResult> transferAsync(UUID from, UUID to, CurrencyType currency, BigDecimal amount, boolean applyTax);
    
    ConversionResult convert(UUID player, CurrencyType from, CurrencyType to, BigDecimal amount);
    
    CompletableFuture<ConversionResult> convertAsync(UUID player, CurrencyType from, CurrencyType to, BigDecimal amount);
}