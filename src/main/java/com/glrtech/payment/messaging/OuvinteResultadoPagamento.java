package com.glrtech.payment.messaging;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.glrtech.payment.domain.EventoPagamento;
import com.glrtech.payment.domain.Pagamento;
import com.glrtech.payment.domain.StatusPagamento;
import com.glrtech.payment.domain.TipoEventoPagamento;
import com.glrtech.payment.messaging.event.MensagemPagamentoConfirmado;
import com.glrtech.payment.messaging.event.MensagemPagamentoFalhou;
import com.glrtech.payment.repository.RepositorioEventoPagamento;
import com.glrtech.payment.repository.RepositorioPagamento;

/**
 * Consome os eventos de confirmação (callback simulado) do gateway e aplica a transição de
 * estado + o registro de auditoria correspondente no pagamento.
 */
@Component
public class OuvinteResultadoPagamento {

    private final RepositorioPagamento repositorioPagamento;
    private final RepositorioEventoPagamento repositorioEventoPagamento;

    public OuvinteResultadoPagamento(
            RepositorioPagamento repositorioPagamento,
            RepositorioEventoPagamento repositorioEventoPagamento) {
        this.repositorioPagamento = repositorioPagamento;
        this.repositorioEventoPagamento = repositorioEventoPagamento;
    }

    @RabbitListener(queues = ConfiguracaoRabbitMQ.QUEUE_CONFIRMED)
    @Transactional
    public void aoConfirmar(MensagemPagamentoConfirmado mensagem) {
        Pagamento pagamento = repositorioPagamento.findById(mensagem.idPagamento())
                .orElseThrow(() -> new IllegalStateException("Pagamento não encontrado: " + mensagem.idPagamento()));

        pagamento.setStatus(StatusPagamento.CONFIRMADO);
        repositorioPagamento.save(pagamento);
        registrarEvento(pagamento.getId(), TipoEventoPagamento.PAGAMENTO_CONFIRMADO,
                "idTransacao=" + mensagem.idTransacao() + "; " + mensagem.mensagem());
    }

    @RabbitListener(queues = ConfiguracaoRabbitMQ.QUEUE_FAILED)
    @Transactional
    public void aoFalhar(MensagemPagamentoFalhou mensagem) {
        Pagamento pagamento = repositorioPagamento.findById(mensagem.idPagamento())
                .orElseThrow(() -> new IllegalStateException("Pagamento não encontrado: " + mensagem.idPagamento()));

        pagamento.setStatus(StatusPagamento.FALHOU);
        repositorioPagamento.save(pagamento);
        registrarEvento(pagamento.getId(), TipoEventoPagamento.PAGAMENTO_FALHOU, mensagem.motivo());
    }

    private void registrarEvento(UUID idPagamento, TipoEventoPagamento tipo, String detalhes) {
        repositorioEventoPagamento.save(EventoPagamento.builder()
                .pagamentoId(idPagamento)
                .tipoEvento(tipo)
                .detalhes(detalhes)
                .build());
    }
}
