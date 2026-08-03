package com.glrtech.payment.gateway;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.glrtech.payment.domain.Payment;

@Component("stripe")
public class SimulatedStripeGateway implements PaymentGateway {

    @Override
    public String name() {
        return "stripe";
    }

    @Override
    public GatewayResult charge(Payment payment) {
        simulateLatency();

        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 5) {
            throw new PaymentGatewayException("Stripe (simulated) timeout while charging payment " + payment.getId());
        }
        if (roll < 20) {
            return GatewayResult.declined("Declined by Stripe (simulated): insufficient funds");
        }
        return GatewayResult.approved(UUID.randomUUID().toString(), "Approved by Stripe (simulated)");
    }

    private void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(150, 400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
