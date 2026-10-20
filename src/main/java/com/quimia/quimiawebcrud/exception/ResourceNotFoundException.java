package com.quimia.quimiawebcrud.exception;

/**
 * Lançada quando um registro buscado pelo identificador não existe no banco.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
