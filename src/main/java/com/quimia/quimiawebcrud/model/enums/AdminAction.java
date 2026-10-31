package com.quimia.quimiawebcrud.model.enums;

/**
 * Tipo de alteração registrada no log de edições. São os únicos valores que a regra {@code chk_acao} do banco aceita;
 * por isso uma restauração é gravada como INSERT (ver {@link com.quimia.quimiawebcrud.service.RemovalLogFormat}).
 */
public enum AdminAction {
    INSERT,
    UPDATE,
    DELETE
}
