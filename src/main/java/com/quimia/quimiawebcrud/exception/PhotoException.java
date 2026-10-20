package com.quimia.quimiawebcrud.exception;

/**
 * Lançada quando a foto enviada não pode ser aceita ou salva; a mensagem é mostrada ao usuário.
 */
public class PhotoException extends RuntimeException {

    public PhotoException(String message) {
        super(message);
    }
}
