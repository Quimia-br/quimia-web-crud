package com.quimia.quimiawebcrud.model.enums;

/**
 * Ação registrada no log de edições. Uma restauração é gravada como INSERT, com um texto próprio em
 * {@code dado_anterior} que a identifica.
 */
public enum AdminAction {
    INSERT,
    UPDATE,
    DELETE
}
