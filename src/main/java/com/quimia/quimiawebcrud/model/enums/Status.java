package com.quimia.quimiawebcrud.model.enums;

/**
 * Situação da conta de administradores, usuários e empresas. PENDENTE e BLOQUEADO são as contas que pedem atenção
 * (aparecem nos alertas do dashboard).
 */
public enum Status {
    ATIVO,
    // Regra a confirmar: o código não diferencia INATIVO de DESATIVADO em nenhuma regra.
    INATIVO,
    PENDENTE,
    BLOQUEADO,
    DESATIVADO
}
