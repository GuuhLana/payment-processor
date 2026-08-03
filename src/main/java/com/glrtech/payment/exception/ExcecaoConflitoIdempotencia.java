package com.glrtech.payment.exception;

public class ExcecaoConflitoIdempotencia extends RuntimeException {

    public ExcecaoConflitoIdempotencia(String mensagem) {
        super(mensagem);
    }
}
