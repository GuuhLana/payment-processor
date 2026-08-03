package com.glrtech.payment.gateway;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.glrtech.payment.domain.Payment;

@Component("pagseguro")
public class SimulatedPagSeguroGateway implements PaymentGateway {

    @Override
    public String name() {
        return "pagseguro";
    }

    @Override
    public GatewayResult charge(Payment payment) {
        simulateLatency();

        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 7) {
            throw new PaymentGatewayException("PagSeguro (simulated) connection reset while charging payment " + payment.getId());
        }
        if (roll < 25) {
            return GatewayResult.declined("Declined by PagSeguro (simulated): card refused");
        }
        return GatewayResult.approved(UUID.randomUUID().toString(), "Approved by PagSeguro (simulated)");
    }

    private void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
