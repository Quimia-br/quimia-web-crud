package com.quimia.quimiawebcrud.model.enums;

/**
 * Porte da empresa. Os nomes são gravados por extenso no banco ({@code @Enumerated(STRING)}): renomear um valor
 * aqui exige migrar os dados já gravados.
 */
public enum CompanySize {
    MICRO,
    PEQUENO,
    MEDIO,
    GRANDE,
    DESCONHECIDO
}
