package com.glrtech.payment.api.dto;

import java.time.Instant;

import com.glrtech.payment.domain.EventoPagamento;
import com.glrtech.payment.domain.TipoEventoPagamento;

public record RespostaEventoPagamento(
        Long id,
        TipoEventoPagamento tipoEvento,
        String detalhes,
        Instant criadoEm) {

    public static RespostaEventoPagamento de(EventoPagamento evento) {
        return new RespostaEventoPagamento(evento.getId(), evento.getTipoEvento(), evento.getDetalhes(), evento.getCriadoEm());
    }
}
