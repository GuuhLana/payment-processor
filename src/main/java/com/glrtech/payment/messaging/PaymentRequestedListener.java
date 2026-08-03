package com.glrtech.payment.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.glrtech.payment.domain.Payment;
import com.glrtech.payment.domain.PaymentEvent;
import com.glrtech.payment.domain.PaymentEventType;
import com.glrtech.payment.domain.PaymentStatus;
import com.glrtech.payment.gateway.GatewayResult;
import com.glrtech.payment.gateway.PaymentGateway;
import com.glrtech.payment.gateway.PaymentGatewayResolver;
import com.glrtech.payment.messaging.event.PaymentConfirmedMessage;
import com.glrtech.payment.messaging.event.PaymentFailedMessage;
import com.glrtech.payment.messaging.event.PaymentRequestedMessage;
import com.glrtech.payment.repository.PaymentEventRepository;
import com.glrtech.payment.repository.PaymentRepository;

/**
 * Consumes PaymentRequested, calls the resolved (simulated) gateway, and publishes the
 * outcome as a PaymentConfirmed/PaymentFailed event. A PaymentGatewayException thrown here
 * propagates out of the listener, triggering the retry-with-backoff + DLQ pipeline configured
 * in RabbitMQConfig — it represents a transient infra failure, not a business decline.
 */
@Component
public class PaymentRequestedListener {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PaymentGatewayResolver gatewayResolver;
    private final PaymentEventPublisher publisher;

    public PaymentRequestedListener(
            PaymentRepository paymentRepository,
            PaymentEventRepository paymentEventRepository,
            PaymentGatewayResolver gatewayResolver,
            PaymentEventPublisher publisher) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.gatewayResolver = gatewayResolver;
        this.publisher = publisher;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_REQUESTED)
    @Transactional
    public void handle(PaymentRequestedMessage message) {
        Payment payment = paymentRepository.findById(message.paymentId())
                .orElseThrow(() -> new IllegalStateException("Payment not found: " + message.paymentId()));

        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
        recordEvent(payment.getId(), PaymentEventType.PAYMENT_PROCESSING,
                "Sending to gateway '" + payment.getGateway() + "'");

        PaymentGateway gateway = gatewayResolver.resolve(payment.getGateway());
        GatewayResult result = gateway.charge(payment);

        if (result.approved()) {
            publisher.publishPaymentConfirmed(
                    new PaymentConfirmedMessage(payment.getId(), result.transactionId(), result.message()));
        } else {
            publisher.publishPaymentFailed(new PaymentFailedMessage(payment.getId(), result.message()));
        }
    }

    private void recordEvent(java.util.UUID paymentId, PaymentEventType type, String details) {
        paymentEventRepository.save(PaymentEvent.builder()
                .paymentId(paymentId)
                .eventType(type)
                .details(details)
                .build());
    }
}
