package com.glrtech.payment.gateway;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ResolvedorGatewayPagamento {

    private final Map<String, GatewayPagamento> gatewaysPorNome;

    public ResolvedorGatewayPagamento(Map<String, GatewayPagamento> gatewaysPorNome) {
        this.gatewaysPorNome = gatewaysPorNome;
    }

    public GatewayPagamento resolver(String nomeGateway) {
        GatewayPagamento gateway = gatewaysPorNome.get(nomeGateway);
        if (gateway == null) {
            throw new IllegalArgumentException("Gateway de pagamento desconhecido: " + nomeGateway
                    + ". Disponíveis: " + gatewaysPorNome.keySet());
        }
        return gateway;
    }
}
