package com.glrtech.payment.exception;

import java.util.UUID;

public class ExcecaoPagamentoNaoEncontrado extends RuntimeException {

    public ExcecaoPagamentoNaoEncontrado(UUID idPagamento) {
        super("Pagamento não encontrado: " + idPagamento);
    }
}
