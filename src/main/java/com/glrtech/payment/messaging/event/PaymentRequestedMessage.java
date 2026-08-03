package com.glrtech.payment.messaging.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestedMessage(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String gateway,
        String idempotencyKey) {
}
