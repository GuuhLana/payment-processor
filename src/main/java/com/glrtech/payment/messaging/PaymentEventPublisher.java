package com.glrtech.payment.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.glrtech.payment.messaging.event.PaymentConfirmedMessage;
import com.glrtech.payment.messaging.event.PaymentFailedMessage;
import com.glrtech.payment.messaging.event.PaymentRequestedMessage;

@Component
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentRequested(PaymentRequestedMessage message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_REQUESTED, message);
    }

    public void publishPaymentConfirmed(PaymentConfirmedMessage message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_CONFIRMED, message);
    }

    public void publishPaymentFailed(PaymentFailedMessage message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_FAILED, message);
    }
}
