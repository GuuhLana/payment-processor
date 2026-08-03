package com.glrtech.payment.api.dto;

import java.time.Instant;

import com.glrtech.payment.domain.PaymentEvent;
import com.glrtech.payment.domain.PaymentEventType;

public record PaymentEventResponse(
        Long id,
        PaymentEventType eventType,
        String details,
        Instant createdAt) {

    public static PaymentEventResponse from(PaymentEvent event) {
        return new PaymentEventResponse(event.getId(), event.getEventType(), event.getDetails(), event.getCreatedAt());
    }
}
