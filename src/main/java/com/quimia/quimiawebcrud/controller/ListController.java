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

    /** Todos os registros da tela, antes dos filtros e da paginação. */
    protected abstract List<Res> readAll();

    /** Rota da tela, usada nos links de paginação e nos redirecionamentos (ex.: "/users"). */
    protected abstract String getBasePath();

    /** Pasta do template da tela em templates/ (ex.: "user" para user/list.html). */
    protected abstract String getViewFolder();

    /** Nome com que a lista da página vai no Model (ex.: "users"). */
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

    /** Abre a lista: filtra pelo Predicate da tela, recorta a página pedida e entrega tudo ao template. */
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
