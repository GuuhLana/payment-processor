package com.glrtech.payment.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.glrtech.payment.api.dto.RequisicaoCriacaoPagamento;
import com.glrtech.payment.api.dto.RespostaEventoPagamento;
import com.glrtech.payment.api.dto.RespostaPagamento;
import com.glrtech.payment.service.ServicoPagamento;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/pagamentos")
public class ControladorPagamento {

    private final ServicoPagamento servicoPagamento;

    public ControladorPagamento(ServicoPagamento servicoPagamento) {
        this.servicoPagamento = servicoPagamento;
    }

    @PostMapping
    public ResponseEntity<RespostaPagamento> criar(@Valid @RequestBody RequisicaoCriacaoPagamento requisicao) {
        RespostaPagamento resposta = servicoPagamento.criarPagamento(requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping("/{id}")
    public RespostaPagamento buscar(@PathVariable UUID id) {
        return servicoPagamento.buscarPagamento(id);
    }

    @GetMapping("/{id}/eventos")
    public List<RespostaEventoPagamento> eventos(@PathVariable UUID id) {
        return servicoPagamento.buscarEventos(id);
    }
}
