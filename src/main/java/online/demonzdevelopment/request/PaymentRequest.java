package online.demonzdevelopment.request;

import online.demonzdevelopment.currency.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentRequest {
    private final UUID requester;
    private final UUID target;
    private final CurrencyType currency;
    private final BigDecimal amount;
    private final long createdAt;

    public PaymentRequest(UUID requester, UUID target, CurrencyType currency, BigDecimal amount) {
        this.requester = requester;
        this.target = target;
        this.currency = currency;
        this.amount = amount;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getRequester() {
        return requester;
    }

    public UUID getTarget() {
        return target;
    }

    public CurrencyType getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > 120000;
    }
}