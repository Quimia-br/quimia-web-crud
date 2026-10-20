package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.BaseResponseDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexa listas por id para as telas mostrarem o nome de um registro relacionado (ex.: nome da empresa de um produto).
 */
final class Lookups {

    private Lookups() {
    }

    static <R extends BaseResponseDTO> Map<Integer, R> byId(List<R> items) {
        Map<Integer, R> map = new LinkedHashMap<>();
        for (R item : items) {
            map.put(item.getId(), item);
        }
        return map;
    }
}
