package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.HistoryTypeRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryTypeResponseDTO;
import com.quimia.quimiawebcrud.model.HistoryType;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.HistoryTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
