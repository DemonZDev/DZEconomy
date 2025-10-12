package online.demonzdevelopment.api;

import java.math.BigDecimal;

public class TransactionResult {
    private final boolean success;
    private final String message;
    private final BigDecimal preBalance;
    private final BigDecimal postBalance;
    private final BigDecimal tax;

    public TransactionResult(boolean success, String message, BigDecimal preBalance, BigDecimal postBalance, BigDecimal tax) {
        this.success = success;
        this.message = message;
        this.preBalance = preBalance;
        this.postBalance = postBalance;
        this.tax = tax;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public BigDecimal getPreBalance() {
        return preBalance;
    }

    public BigDecimal getPostBalance() {
        return postBalance;
    }

    public BigDecimal getTax() {
        return tax;
    }
}