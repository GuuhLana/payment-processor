package com.glrtech.payment.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the async payment pipeline:
 *   payments.exchange (topic)
 *     -> payment.requested.queue   (consumed by the gateway-calling listener)
 *     -> payment.confirmed.queue   (consumed by the outcome listener)
 *     -> payment.failed.queue      (consumed by the outcome listener)
 *
 * The requested-queue listener carries a retry interceptor with exponential backoff;
 * once attempts are exhausted, RepublishMessageRecoverer routes the message to
 * payments.dlx -> payment.requested.dlq instead of losing it.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "payments.exchange";
    public static final String DLX = "payments.dlx";

    public static final String ROUTING_KEY_REQUESTED = "payment.requested";
    public static final String ROUTING_KEY_CONFIRMED = "payment.confirmed";
    public static final String ROUTING_KEY_FAILED = "payment.failed";
    public static final String DLQ_ROUTING_KEY = "payment.requested.dlq";

    public static final String QUEUE_REQUESTED = "payment.requested.queue";
    public static final String QUEUE_CONFIRMED = "payment.confirmed.queue";
    public static final String QUEUE_FAILED = "payment.failed.queue";
    public static final String DLQ_REQUESTED = "payment.requested.dlq";

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue paymentRequestedQueue() {
        return QueueBuilder.durable(QUEUE_REQUESTED).build();
    }

    @Bean
    public Queue paymentConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_CONFIRMED).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(QUEUE_FAILED).build();
    }

    @Bean
    public Queue paymentRequestedDlq() {
        return QueueBuilder.durable(DLQ_REQUESTED).build();
    }

    @Bean
    public Binding bindRequestedQueue(Queue paymentRequestedQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(paymentRequestedQueue).to(paymentsExchange).with(ROUTING_KEY_REQUESTED);
    }

    @Bean
    public Binding bindConfirmedQueue(Queue paymentConfirmedQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(paymentConfirmedQueue).to(paymentsExchange).with(ROUTING_KEY_CONFIRMED);
    }

    @Bean
    public Binding bindFailedQueue(Queue paymentFailedQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(paymentFailedQueue).to(paymentsExchange).with(ROUTING_KEY_FAILED);
    }

    @Bean
    public Binding bindDlq(Queue paymentRequestedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(paymentRequestedDlq).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter("com.glrtech.payment.messaging.event");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public RepublishMessageRecoverer republishMessageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLX, DLQ_ROUTING_KEY);
    }

    /**
     * 4 attempts total: immediate try + retries at ~1s, ~2s, ~4s (capped at 10s), then dead-lettered.
     */
    @Bean
    public StatelessRetryOperationsInterceptor retryInterceptor(RepublishMessageRecoverer republishMessageRecoverer) {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(republishMessageRecoverer)
                .build();
    }

    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter converter,
            StatelessRetryOperationsInterceptor retryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAdviceChain(retryInterceptor);
        return factory;
    }
}
