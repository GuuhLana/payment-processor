package com.glrtech.payment.gateway;

/**
 * Simulates a transient/infrastructure failure talking to the gateway (timeout, connection reset),
 * as opposed to a business decline. Thrown from inside a message listener, this is what
 * drives the retry-with-backoff + DLQ pipeline.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }
}
