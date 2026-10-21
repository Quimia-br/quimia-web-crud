package com.quimia.quimiawebcrud.exception;

/**
 * Base das exceções com mensagem própria para o usuário (regra de negócio, dado inválido, foto recusada...).
 * Qualquer outra exceção é tratada como erro inesperado.
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }
}
