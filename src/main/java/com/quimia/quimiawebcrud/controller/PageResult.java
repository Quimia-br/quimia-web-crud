package com.quimia.quimiawebcrud.controller;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Uma página de uma lista: os itens dela e os dados para montar a navegação (anterior, números, próxima).
 */
@Getter
public class PageResult<T> {

    private static final int WINDOW_RADIUS = 2;

    private final List<T> items;
    private final int number;
    private final int totalPages;

    private PageResult(List<T> items, int number, int totalPages) {
        this.items = items;
        this.number = number;
        this.totalPages = totalPages;
    }

    /**
     * Recorta a página pedida (começa em 1); número fora do intervalo vira a primeira ou a última página.
     */
    public static <T> PageResult<T> of(List<T> all, int requestedPage, int pageSize) {
        int totalPages = Math.max(1, (all.size() + pageSize - 1) / pageSize);
        int number = Math.min(Math.max(requestedPage, 1), totalPages);
        int from = (number - 1) * pageSize;
        int to = Math.min(from + pageSize, all.size());
        return new PageResult<>(new ArrayList<>(all.subList(from, to)), number, totalPages);
    }

    public boolean hasPrevious() {
        return number > 1;
    }

    public boolean hasNext() {
        return number < totalPages;
    }

    /**
     * Números de página exibidos: a atual e até duas de cada lado.
     */
    public List<Integer> getWindow() {
        List<Integer> pages = new ArrayList<>();
        for (int p = Math.max(1, number - WINDOW_RADIUS); p <= Math.min(totalPages, number + WINDOW_RADIUS); p++) {
            pages.add(p);
        }
        return pages;
    }
}
