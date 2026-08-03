package com.glrtech.payment.messaging.event;

import java.util.UUID;

public record PaymentConfirmedMessage(
        UUID paymentId,
        String transactionId,
        String message) {
}
