package com.glrtech.payment.gateway;

public record ResultadoGateway(boolean aprovado, String idTransacao, String mensagem) {

    public static ResultadoGateway aprovado(String idTransacao, String mensagem) {
        return new ResultadoGateway(true, idTransacao, mensagem);
    }

    public static ResultadoGateway recusado(String mensagem) {
        return new ResultadoGateway(false, null, mensagem);
    }
}
