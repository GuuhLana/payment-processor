package com.glrtech.payment.gateway;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.glrtech.payment.domain.Pagamento;

@Component("stripe")
public class GatewaySimuladoStripe implements GatewayPagamento {

    @Override
    public String nome() {
        return "stripe";
    }

    @Override
    public ResultadoGateway cobrar(Pagamento pagamento) {
        simularLatencia();

        int sorteio = ThreadLocalRandom.current().nextInt(100);
        if (sorteio < 5) {
            throw new ExcecaoGatewayPagamento("Timeout do Stripe (simulado) ao cobrar o pagamento " + pagamento.getId());
        }
        if (sorteio < 20) {
            return ResultadoGateway.recusado("Recusado pelo Stripe (simulado): saldo insuficiente");
        }
        return ResultadoGateway.aprovado(UUID.randomUUID().toString(), "Aprovado pelo Stripe (simulado)");
    }

    private void simularLatencia() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(150, 400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
