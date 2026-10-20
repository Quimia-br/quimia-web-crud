package com.quimia.quimiawebcrud.exception;

/**
 * Lançada quando se tenta criar um registro que já existe (ex.: produto repetido na mesma estante).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
