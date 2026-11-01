package com.quimia.quimiawebcrud.model.enums;

/**
 * Situação de uma conta (usuário, empresa ou administrador). Os nomes são gravados por extenso no banco
 * ({@code @Enumerated(STRING)}): renomear um valor aqui exige migrar os dados já gravados.
 */
public enum Status {
    ATIVO,
    INATIVO,
    PENDENTE,
    BLOQUEADO,
    DESATIVADO
}
