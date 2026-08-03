package com.glrtech.payment.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RequisicaoCriacaoPagamento(
        @NotNull @Positive BigDecimal valor,
        @NotBlank String moeda,
        @NotBlank String gateway,
        @NotBlank String chaveIdempotencia) {
}
