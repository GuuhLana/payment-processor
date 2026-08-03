package com.glrtech.payment.messaging;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.glrtech.payment.domain.Payment;
import com.glrtech.payment.domain.PaymentEvent;
import com.glrtech.payment.domain.PaymentEventType;
import com.glrtech.payment.domain.PaymentStatus;
import com.glrtech.payment.messaging.event.PaymentConfirmedMessage;
import com.glrtech.payment.messaging.event.PaymentFailedMessage;
import com.glrtech.payment.repository.PaymentEventRepository;
import com.glrtech.payment.repository.PaymentRepository;

/**
 * Consumes the gateway's (simulated) confirmation callback events and applies the
 * resulting state transition + audit entry to the payment.
 */
@Component
public class PaymentOutcomeListener {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentOutcomeListener(PaymentRepository paymentRepository, PaymentEventRepository paymentEventRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CONFIRMED)
    @Transactional
    public void onConfirmed(PaymentConfirmedMessage message) {
        Payment payment = paymentRepository.findById(message.paymentId())
                .orElseThrow(() -> new IllegalStateException("Payment not found: " + message.paymentId()));

        payment.setStatus(PaymentStatus.CONFIRMED);
        paymentRepository.save(payment);
        recordEvent(payment.getId(), PaymentEventType.PAYMENT_CONFIRMED,
                "transactionId=" + message.transactionId() + "; " + message.message());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_FAILED)
    @Transactional
    public void onFailed(PaymentFailedMessage message) {
        Payment payment = paymentRepository.findById(message.paymentId())
                .orElseThrow(() -> new IllegalStateException("Payment not found: " + message.paymentId()));

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        recordEvent(payment.getId(), PaymentEventType.PAYMENT_FAILED, message.reason());
    }

    private void recordEvent(UUID paymentId, PaymentEventType type, String details) {
        paymentEventRepository.save(PaymentEvent.builder()
                .paymentId(paymentId)
                .eventType(type)
                .details(details)
                .build());
    }
}
