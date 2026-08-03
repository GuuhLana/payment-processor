package com.glrtech.payment.messaging.event;

import java.math.BigDecimal;
import java.util.UUID;

public record MensagemPagamentoSolicitado(
        UUID idPagamento,
        BigDecimal valor,
        String moeda,
        String gateway,
        String chaveIdempotencia) {
}
