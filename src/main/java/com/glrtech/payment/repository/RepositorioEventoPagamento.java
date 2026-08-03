package com.glrtech.payment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.glrtech.payment.domain.EventoPagamento;

public interface RepositorioEventoPagamento extends JpaRepository<EventoPagamento, Long> {

    List<EventoPagamento> findByPagamentoIdOrderByCriadoEmAsc(UUID pagamentoId);
}
