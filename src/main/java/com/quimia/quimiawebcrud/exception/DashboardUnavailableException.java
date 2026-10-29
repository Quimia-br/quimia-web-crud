package com.quimia.quimiawebcrud.exception;

/**
 * Lançada quando os números do dashboard não podem ser lidos (ex.: a view materializada não existe ou ainda não
 * foi populada).
 */
public class DashboardUnavailableException extends BusinessException {

    public DashboardUnavailableException(String message) {
        super(message);
    }
}
