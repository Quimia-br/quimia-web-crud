package com.quimia.quimiawebcrud.exception;

/**
 * Lançada quando os dados enviados violam uma regra de negócio (ex.: senha obrigatória ausente).
 */
public class InvalidDataException extends BusinessException {

    public InvalidDataException(String message) {
        super(message);
    }
}
