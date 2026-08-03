package com.glrtech.payment.messaging.event;

import java.util.UUID;

public record MensagemPagamentoFalhou(
        UUID idPagamento,
        String motivo) {
}
