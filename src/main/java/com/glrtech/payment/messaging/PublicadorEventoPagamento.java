package com.glrtech.payment.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.glrtech.payment.messaging.event.MensagemPagamentoConfirmado;
import com.glrtech.payment.messaging.event.MensagemPagamentoFalhou;
import com.glrtech.payment.messaging.event.MensagemPagamentoSolicitado;

@Component
public class PublicadorEventoPagamento {

    private final RabbitTemplate rabbitTemplate;

    public PublicadorEventoPagamento(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarPagamentoSolicitado(MensagemPagamentoSolicitado mensagem) {
        rabbitTemplate.convertAndSend(ConfiguracaoRabbitMQ.EXCHANGE, ConfiguracaoRabbitMQ.ROUTING_KEY_REQUESTED, mensagem);
    }

    public void publicarPagamentoConfirmado(MensagemPagamentoConfirmado mensagem) {
        rabbitTemplate.convertAndSend(ConfiguracaoRabbitMQ.EXCHANGE, ConfiguracaoRabbitMQ.ROUTING_KEY_CONFIRMED, mensagem);
    }

    public void publicarPagamentoFalhou(MensagemPagamentoFalhou mensagem) {
        rabbitTemplate.convertAndSend(ConfiguracaoRabbitMQ.EXCHANGE, ConfiguracaoRabbitMQ.ROUTING_KEY_FAILED, mensagem);
    }
}
