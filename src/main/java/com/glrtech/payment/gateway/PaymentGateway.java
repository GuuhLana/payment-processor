package com.glrtech.payment.gateway;

import com.glrtech.payment.domain.Payment;

/**
 * Strategy/Adapter interface: each concrete gateway adapts the payment domain to a
 * (simulated) external provider's charging semantics.
 */
public interface PaymentGateway {

    String name();

    GatewayResult charge(Payment payment);
}
