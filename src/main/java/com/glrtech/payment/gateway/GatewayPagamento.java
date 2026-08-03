package com.glrtech.payment.gateway;

import com.glrtech.payment.domain.Pagamento;

/**
 * Interface Strategy/Adapter: cada gateway concreto adapta o domínio de pagamento para a
 * semântica de cobrança de um provedor (simulado) diferente.
 */
public interface GatewayPagamento {

    String nome();

    ResultadoGateway cobrar(Pagamento pagamento);
}
