package com.glrtech.payment.gateway;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.glrtech.payment.domain.Pagamento;

@Component("pagseguro")
public class GatewaySimuladoPagSeguro implements GatewayPagamento {

    @Override
    public String nome() {
        return "pagseguro";
    }

    @Override
    public ResultadoGateway cobrar(Pagamento pagamento) {
        simularLatencia();

        int sorteio = ThreadLocalRandom.current().nextInt(100);
        if (sorteio < 7) {
            throw new ExcecaoGatewayPagamento("Conexão resetada do PagSeguro (simulado) ao cobrar o pagamento " + pagamento.getId());
        }
        if (sorteio < 25) {
            return ResultadoGateway.recusado("Recusado pelo PagSeguro (simulado): cartão recusado");
        }
        return ResultadoGateway.aprovado(UUID.randomUUID().toString(), "Aprovado pelo PagSeguro (simulado)");
    }

    private void simularLatencia() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
