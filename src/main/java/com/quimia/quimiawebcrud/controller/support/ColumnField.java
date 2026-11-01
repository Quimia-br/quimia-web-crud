package com.quimia.quimiawebcrud.controller.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Uma coluna que os popups "Atualizar por..." e "Apagar por..." de uma tela oferecem: o nome no banco, o rótulo
 * mostrado e se ela também pode receber um novo valor (nem toda coluna faz sentido alterada em várias linhas).
 */
@Getter
@RequiredArgsConstructor
public final class ColumnField {

    private final String column;
    private final String label;
    private final boolean updatable;

    /** Valores aceitos por uma coluna de lista (enum), para o rótulo mostrar quais digitar. */
    public static String options(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
    }

    /** Coluna que só serve para escolher os registros. */
    public static ColumnField filter(String column, String label) {
        return new ColumnField(column, label, false);
    }

    /** Coluna que serve para escolher os registros e também para receber um novo valor. */
    public static ColumnField editable(String column, String label) {
        return new ColumnField(column, label, true);
    }
}
