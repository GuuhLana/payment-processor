package com.glrtech.payment.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.glrtech.payment.api.dto.RequisicaoCriacaoPagamento;
import com.glrtech.payment.api.dto.RespostaEventoPagamento;
import com.glrtech.payment.api.dto.RespostaPagamento;
import com.glrtech.payment.domain.EventoPagamento;
import com.glrtech.payment.domain.Pagamento;
import com.glrtech.payment.domain.StatusPagamento;
import com.glrtech.payment.domain.TipoEventoPagamento;
import com.glrtech.payment.exception.ExcecaoConflitoIdempotencia;
import com.glrtech.payment.exception.ExcecaoPagamentoNaoEncontrado;
import com.glrtech.payment.gateway.ResolvedorGatewayPagamento;
import com.glrtech.payment.messaging.PublicadorEventoPagamento;
import com.glrtech.payment.messaging.event.MensagemPagamentoSolicitado;
import com.glrtech.payment.repository.RepositorioEventoPagamento;
import com.glrtech.payment.repository.RepositorioPagamento;

@Service
public class ServicoPagamento {

    private final RepositorioPagamento repositorioPagamento;
    private final RepositorioEventoPagamento repositorioEventoPagamento;
    private final PublicadorEventoPagamento publicador;
    private final ResolvedorGatewayPagamento resolvedorGateway;

    public ServicoPagamento(
            RepositorioPagamento repositorioPagamento,
            RepositorioEventoPagamento repositorioEventoPagamento,
            PublicadorEventoPagamento publicador,
            ResolvedorGatewayPagamento resolvedorGateway) {
        this.repositorioPagamento = repositorioPagamento;
        this.repositorioEventoPagamento = repositorioEventoPagamento;
        this.publicador = publicador;
        this.resolvedorGateway = resolvedorGateway;
    }

    @Transactional
    public RespostaPagamento criarPagamento(RequisicaoCriacaoPagamento requisicao) {
        resolvedorGateway.resolver(requisicao.gateway()); // falha rápido para gateway desconhecido (400),
                                                           // em vez de queimar retries e cair na DLQ

        Optional<Pagamento> existente = repositorioPagamento.findByChaveIdempotencia(requisicao.chaveIdempotencia());
        if (existente.isPresent()) {
            return reproduzirOuConflitar(existente.get(), requisicao);
        }

        Pagamento pagamento = Pagamento.builder()
                .id(UUID.randomUUID())
                .valor(requisicao.valor())
                .moeda(requisicao.moeda())
                .gateway(requisicao.gateway())
                .status(StatusPagamento.PENDENTE)
                .chaveIdempotencia(requisicao.chaveIdempotencia())
                .build();

        try {
            pagamento = repositorioPagamento.saveAndFlush(pagamento);
        } catch (DataIntegrityViolationException perdeuCorrida) {
            // Outra requisição venceu a corrida pela constraint única de chave_idempotencia.
            return repositorioPagamento.findByChaveIdempotencia(requisicao.chaveIdempotencia())
                    .map(RespostaPagamento::de)
                    .orElseThrow(() -> perdeuCorrida);
        }

        registrarEvento(pagamento.getId(), TipoEventoPagamento.PAGAMENTO_SOLICITADO, "Pagamento criado com status PENDENTE");

        publicador.publicarPagamentoSolicitado(new MensagemPagamentoSolicitado(
                pagamento.getId(), pagamento.getValor(), pagamento.getMoeda(),
                pagamento.getGateway(), pagamento.getChaveIdempotencia()));

        return RespostaPagamento.de(pagamento);
    }

    private RespostaPagamento reproduzirOuConflitar(Pagamento existente, RequisicaoCriacaoPagamento requisicao) {
        boolean mesmaRequisicao = existente.getValor().compareTo(requisicao.valor()) == 0
                && existente.getMoeda().equals(requisicao.moeda())
                && existente.getGateway().equals(requisicao.gateway());

        if (!mesmaRequisicao) {
            throw new ExcecaoConflitoIdempotencia(
                    "chaveIdempotencia '" + requisicao.chaveIdempotencia() + "' já foi usada com dados de pagamento diferentes");
        }
        return RespostaPagamento.de(existente);
    }

    @Transactional(readOnly = true)
    public RespostaPagamento buscarPagamento(UUID id) {
        return repositorioPagamento.findById(id)
                .map(RespostaPagamento::de)
                .orElseThrow(() -> new ExcecaoPagamentoNaoEncontrado(id));
    }

    @Transactional(readOnly = true)
    public List<RespostaEventoPagamento> buscarEventos(UUID id) {
        if (!repositorioPagamento.existsById(id)) {
            throw new ExcecaoPagamentoNaoEncontrado(id);
        }
        return repositorioEventoPagamento.findByPagamentoIdOrderByCriadoEmAsc(id).stream()
                .map(RespostaEventoPagamento::de)
                .toList();
    }

    private void registrarEvento(UUID idPagamento, TipoEventoPagamento tipo, String detalhes) {
        repositorioEventoPagamento.save(EventoPagamento.builder()
                .pagamentoId(idPagamento)
                .tipoEvento(tipo)
                .detalhes(detalhes)
                .build());
    }
}
