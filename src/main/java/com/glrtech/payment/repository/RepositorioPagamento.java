package com.glrtech.payment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.glrtech.payment.domain.Pagamento;

public interface RepositorioPagamento extends JpaRepository<Pagamento, UUID> {

    Optional<Pagamento> findByChaveIdempotencia(String chaveIdempotencia);
}
