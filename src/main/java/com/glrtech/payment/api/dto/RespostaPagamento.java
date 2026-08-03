package com.glrtech.payment.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.glrtech.payment.domain.Pagamento;
import com.glrtech.payment.domain.StatusPagamento;

public record RespostaPagamento(
        UUID id,
        BigDecimal valor,
        String moeda,
        String gateway,
        StatusPagamento status,
        String chaveIdempotencia,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static RespostaPagamento de(Pagamento pagamento) {
        return new RespostaPagamento(
                pagamento.getId(),
                pagamento.getValor(),
                pagamento.getMoeda(),
                pagamento.getGateway(),
                pagamento.getStatus(),
                pagamento.getChaveIdempotencia(),
                pagamento.getCriadoEm(),
                pagamento.getAtualizadoEm());
    }
}
