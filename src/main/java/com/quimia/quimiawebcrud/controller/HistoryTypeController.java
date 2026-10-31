package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.HistoryTypeRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryTypeResponseDTO;
import com.quimia.quimiawebcrud.model.HistoryType;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.HistoryTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Telas de tipos de histórico em {@code /history-types}: lista, detalhe, cadastro, edição e remoção.
 */
@Controller
@RequestMapping("/history-types")
@RequiredArgsConstructor
public class HistoryTypeController extends GenericController<HistoryType, Integer, HistoryTypeResponseDTO, HistoryTypeRequestDTO> {

    private final HistoryTypeService historyTypeService;

    @Override
    protected CrudService<HistoryType, Integer, HistoryTypeRequestDTO, HistoryTypeResponseDTO> getService() {
        return historyTypeService;
    }

    @Override
    protected String getBasePath() {
        return "/history-types";
    }

    @Override
    protected String getViewFolder() {
        return "history-type";
    }

    @Override
    protected String getPluralName() {
        return "historyTypes";
    }

    @Override
    protected List<ColumnField> getColumnFields() {
        return List.of(ColumnField.filter("nome", "Nome"));
    }

    @Override
    protected Predicate<HistoryTypeResponseDTO> filterBy(Map<String, String> params, Model model) {
        return type -> Filters.matchesText(params.get("busca"), type.getName());
    }
}
