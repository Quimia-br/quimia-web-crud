package com.quimia.quimiawebcrud.controller.support;

import com.quimia.quimiawebcrud.dto.BaseResponseDTO;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexa listas por id para as telas mostrarem o nome de um registro relacionado (ex.: nome da empresa de um produto).
 */
public final class Lookups {

    private Lookups() {
    }

    public static <R extends BaseResponseDTO> Map<Integer, R> byId(List<R> items) {
        Map<Integer, R> map = new LinkedHashMap<>();
        for (R item : items) {
            map.put(item.getId(), item);
        }
        return map;
    }

    /**
     * Recupera um mapa que o controller já colocou no model (ex.: "companiesById").
     */
    @SuppressWarnings("unchecked")
    public static <R> Map<Integer, R> fromModel(Model model, String name) {
        Object value = model.getAttribute(name);
        return value == null ? Collections.emptyMap() : (Map<Integer, R>) value;
    }
}
