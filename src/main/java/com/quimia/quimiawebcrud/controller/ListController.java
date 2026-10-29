package com.quimia.quimiawebcrud.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Lista com filtros e paginação, usada por todas as telas do painel (de cadastro ou só de consulta).
 */
public abstract class ListController<Res> {

    private static final int PAGE_SIZE = 10;

    protected abstract List<Res> readAll();

    protected abstract String getBasePath();

    protected abstract String getViewFolder();

    protected abstract String getPluralName();

    /**
     * Dados extras que a lista precisa (ex.: nomes das empresas dos produtos e opções dos filtros).
     */
    protected void addListAttributes(Model model) {
    }

    /**
     * Regra dos filtros da tela, a partir dos parâmetros da URL. Por padrão não filtra nada.
     */
    protected Predicate<Res> filterBy(Map<String, String> params, Model model) {
        return item -> true;
    }

    /**
     * Colunas dos popups "Atualizar por..." e "Apagar por...". Telas só de consulta não têm nenhuma.
     */
    protected List<ColumnField> getColumnFields() {
        return List.of();
    }

    @GetMapping
    public String list(@RequestParam Map<String, String> params, Model model) {
        addListAttributes(model);
        List<ColumnField> columnFields = getColumnFields();
        model.addAttribute("columnFields", columnFields);
        model.addAttribute("updatableFields", columnFields.stream().filter(ColumnField::isUpdatable).toList());
        List<Res> filtered = readAll().stream().filter(filterBy(params, model)).toList();
        PageResult<Res> page = PageResult.of(filtered, Filters.pageNumber(params.getOrDefault("page", "1")), PAGE_SIZE);
        model.addAttribute(getPluralName(), page.getItems());
        model.addAttribute("page", page);
        model.addAttribute("filters", params);
        model.addAttribute("query", Filters.queryString(params));
        model.addAttribute("basePath", getBasePath());
        return getViewFolder() + "/list";
    }
}
