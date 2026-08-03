package com.glrtech.payment.messaging;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.glrtech.payment.domain.EventoPagamento;
import com.glrtech.payment.domain.Pagamento;
import com.glrtech.payment.domain.StatusPagamento;
import com.glrtech.payment.domain.TipoEventoPagamento;
import com.glrtech.payment.gateway.GatewayPagamento;
import com.glrtech.payment.gateway.ResolvedorGatewayPagamento;
import com.glrtech.payment.gateway.ResultadoGateway;
import com.glrtech.payment.messaging.event.MensagemPagamentoConfirmado;
import com.glrtech.payment.messaging.event.MensagemPagamentoFalhou;
import com.glrtech.payment.messaging.event.MensagemPagamentoSolicitado;
import com.glrtech.payment.repository.RepositorioEventoPagamento;
import com.glrtech.payment.repository.RepositorioPagamento;

/**
 * Consome PagamentoSolicitado, chama o gateway (simulado) resolvido e publica o resultado como
 * evento PagamentoConfirmado/PagamentoFalhou. Uma ExcecaoGatewayPagamento lançada aqui propaga
 * para fora do listener, acionando o pipeline de retry com backoff + DLQ configurado em
 * ConfiguracaoRabbitMQ — ela representa uma falha transitória de infraestrutura, não uma recusa
 * de negócio.
 */
@Component
public class OuvintePagamentoSolicitado {

    private final RepositorioPagamento repositorioPagamento;
    private final RepositorioEventoPagamento repositorioEventoPagamento;
    private final ResolvedorGatewayPagamento resolvedorGateway;
    private final PublicadorEventoPagamento publicador;

    public OuvintePagamentoSolicitado(
            RepositorioPagamento repositorioPagamento,
            RepositorioEventoPagamento repositorioEventoPagamento,
            ResolvedorGatewayPagamento resolvedorGateway,
            PublicadorEventoPagamento publicador) {
        this.repositorioPagamento = repositorioPagamento;
        this.repositorioEventoPagamento = repositorioEventoPagamento;
        this.resolvedorGateway = resolvedorGateway;
        this.publicador = publicador;
    }

    @RabbitListener(queues = ConfiguracaoRabbitMQ.QUEUE_REQUESTED)
    @Transactional
    public void tratar(MensagemPagamentoSolicitado mensagem) {
        Pagamento pagamento = repositorioPagamento.findById(mensagem.idPagamento())
                .orElseThrow(() -> new IllegalStateException("Pagamento não encontrado: " + mensagem.idPagamento()));

        pagamento.setStatus(StatusPagamento.PROCESSANDO);
        repositorioPagamento.save(pagamento);
        registrarEvento(pagamento.getId(), TipoEventoPagamento.PAGAMENTO_PROCESSANDO,
                "Enviando para o gateway '" + pagamento.getGateway() + "'");

        GatewayPagamento gateway = resolvedorGateway.resolver(pagamento.getGateway());
        ResultadoGateway resultado = gateway.cobrar(pagamento);

        if (resultado.aprovado()) {
            publicador.publicarPagamentoConfirmado(
                    new MensagemPagamentoConfirmado(pagamento.getId(), resultado.idTransacao(), resultado.mensagem()));
        } else {
            publicador.publicarPagamentoFalhou(new MensagemPagamentoFalhou(pagamento.getId(), resultado.mensagem()));
        }
    }

    private void registrarEvento(UUID idPagamento, TipoEventoPagamento tipo, String detalhes) {
        repositorioEventoPagamento.save(EventoPagamento.builder()
                .pagamentoId(idPagamento)
                .tipoEvento(tipo)
                .detalhes(detalhes)
                .build());
    }
}
