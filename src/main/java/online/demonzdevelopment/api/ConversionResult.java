package online.demonzdevelopment.api;

import online.demonzdevelopment.currency.CurrencyType;

import java.math.BigDecimal;

public class ConversionResult {
    private final boolean success;
    private final String message;
    private final CurrencyType fromCurrency;
    private final CurrencyType toCurrency;
    private final BigDecimal fromAmount;
    private final BigDecimal toAmount;
    private final BigDecimal tax;

    public ConversionResult(boolean success, String message, CurrencyType fromCurrency, CurrencyType toCurrency, 
                          BigDecimal fromAmount, BigDecimal toAmount, BigDecimal tax) {
        this.success = success;
        this.message = message;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.tax = tax;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public CurrencyType getFromCurrency() {
        return fromCurrency;
    }

    public CurrencyType getToCurrency() {
        return toCurrency;
    }

    public BigDecimal getFromAmount() {
        return fromAmount;
    }

    public BigDecimal getToAmount() {
        return toAmount;
    }

    public BigDecimal getTax() {
        return tax;
    }
}