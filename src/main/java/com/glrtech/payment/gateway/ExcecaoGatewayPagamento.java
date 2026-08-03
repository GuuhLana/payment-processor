package com.glrtech.payment.gateway;

/**
 * Simula uma falha transitória/de infraestrutura ao falar com o gateway (timeout, conexão
 * resetada), diferente de uma recusa de negócio. Lançada de dentro de um listener de mensageria,
 * é o que aciona o pipeline de retry com backoff + DLQ.
 */
public class ExcecaoGatewayPagamento extends RuntimeException {

    public ExcecaoGatewayPagamento(String mensagem) {
        super(mensagem);
    }
}
