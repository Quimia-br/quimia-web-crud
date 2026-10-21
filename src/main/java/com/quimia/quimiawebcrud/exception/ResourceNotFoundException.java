package com.quimia.quimiawebcrud.exception;

/**
 * Lançada quando um registro buscado pelo identificador não existe no banco.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
