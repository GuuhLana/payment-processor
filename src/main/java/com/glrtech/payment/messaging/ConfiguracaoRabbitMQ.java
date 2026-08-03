package com.glrtech.payment.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fiação (wiring) do pipeline assíncrono de pagamentos:
 *   payments.exchange (topic)
 *     -> payment.requested.queue   (consumida pelo listener que chama o gateway)
 *     -> payment.confirmed.queue   (consumida pelo listener de resultado)
 *     -> payment.failed.queue      (consumida pelo listener de resultado)
 *
 * O listener da fila de solicitação carrega um interceptor de retry com backoff exponencial;
 * esgotadas as tentativas, o RepublishMessageRecoverer envia a mensagem para
 * payments.dlx -> payment.requested.dlq em vez de perdê-la.
 *
 * Os nomes de exchange/fila/routing key ficam em inglês por serem termos padrão do AMQP/RabbitMQ.
 */
@Configuration
public class ConfiguracaoRabbitMQ {

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
    public TopicExchange exchangePagamentos() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange exchangeMensagensMortas() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue filaPagamentoSolicitado() {
        return QueueBuilder.durable(QUEUE_REQUESTED).build();
    }

    @Bean
    public Queue filaPagamentoConfirmado() {
        return QueueBuilder.durable(QUEUE_CONFIRMED).build();
    }

    @Bean
    public Queue filaPagamentoFalhou() {
        return QueueBuilder.durable(QUEUE_FAILED).build();
    }

    @Bean
    public Queue filaMortaPagamentoSolicitado() {
        return QueueBuilder.durable(DLQ_REQUESTED).build();
    }

    @Bean
    public Binding vincularFilaSolicitado(Queue filaPagamentoSolicitado, TopicExchange exchangePagamentos) {
        return BindingBuilder.bind(filaPagamentoSolicitado).to(exchangePagamentos).with(ROUTING_KEY_REQUESTED);
    }

    @Bean
    public Binding vincularFilaConfirmado(Queue filaPagamentoConfirmado, TopicExchange exchangePagamentos) {
        return BindingBuilder.bind(filaPagamentoConfirmado).to(exchangePagamentos).with(ROUTING_KEY_CONFIRMED);
    }

    @Bean
    public Binding vincularFilaFalhou(Queue filaPagamentoFalhou, TopicExchange exchangePagamentos) {
        return BindingBuilder.bind(filaPagamentoFalhou).to(exchangePagamentos).with(ROUTING_KEY_FAILED);
    }

    @Bean
    public Binding vincularFilaMorta(Queue filaMortaPagamentoSolicitado, DirectExchange exchangeMensagensMortas) {
        return BindingBuilder.bind(filaMortaPagamentoSolicitado).to(exchangeMensagensMortas).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter conversorJson() {
        return new JacksonJsonMessageConverter("com.glrtech.payment.messaging.event");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter conversor) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(conversor);
        return template;
    }

    @Bean
    public RepublishMessageRecoverer recuperadorPorRepublicacao(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLX, DLQ_ROUTING_KEY);
    }

    /**
     * 4 tentativas no total: tentativa imediata + retries em ~1s, ~2s, ~4s (limitado a 10s),
     * depois vai para a dead-letter queue.
     */
    @Bean
    public StatelessRetryOperationsInterceptor interceptorDeRetry(RepublishMessageRecoverer recuperadorPorRepublicacao) {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(recuperadorPorRepublicacao)
                .build();
    }

    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory fabricaContainerDeListener(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter conversor,
            StatelessRetryOperationsInterceptor interceptorDeRetry) {
        SimpleRabbitListenerContainerFactory fabrica = new SimpleRabbitListenerContainerFactory();
        fabrica.setConnectionFactory(connectionFactory);
        fabrica.setMessageConverter(conversor);
        fabrica.setAdviceChain(interceptorDeRetry);
        return fabrica;
    }
}
