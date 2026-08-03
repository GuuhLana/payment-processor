package com.glrtech.payment.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.glrtech.payment.domain.Payment;
import com.glrtech.payment.domain.PaymentStatus;

public record PaymentResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        String gateway,
        PaymentStatus status,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getGateway(),
                payment.getStatus(),
                payment.getIdempotencyKey(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
