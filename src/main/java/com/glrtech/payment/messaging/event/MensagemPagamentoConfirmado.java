package com.glrtech.payment.messaging.event;

import java.util.UUID;

public record MensagemPagamentoConfirmado(
        UUID idPagamento,
        String idTransacao,
        String mensagem) {
}
