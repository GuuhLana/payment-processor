package com.glrtech.payment.gateway;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayResolver {

    private final Map<String, PaymentGateway> gatewaysByName;

    public PaymentGatewayResolver(Map<String, PaymentGateway> gatewaysByName) {
        this.gatewaysByName = gatewaysByName;
    }

    public PaymentGateway resolve(String gatewayName) {
        PaymentGateway gateway = gatewaysByName.get(gatewayName);
        if (gateway == null) {
            throw new IllegalArgumentException("Unknown payment gateway: " + gatewayName
                    + ". Available: " + gatewaysByName.keySet());
        }
        return gateway;
    }
}
