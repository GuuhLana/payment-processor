package com.glrtech.payment.gateway;

public record GatewayResult(boolean approved, String transactionId, String message) {

    public static GatewayResult approved(String transactionId, String message) {
        return new GatewayResult(true, transactionId, message);
    }

    public static GatewayResult declined(String message) {
        return new GatewayResult(false, null, message);
    }
}
