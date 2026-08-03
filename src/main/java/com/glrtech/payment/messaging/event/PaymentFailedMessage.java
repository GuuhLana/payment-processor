package com.glrtech.payment.messaging.event;

import java.util.UUID;

public record PaymentFailedMessage(
        UUID paymentId,
        String reason) {
}
