package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.HistoryRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryResponseDTO;
import com.quimia.quimiawebcrud.model.History;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.HistoryService;
import com.quimia.quimiawebcrud.service.HistoryTypeService;
import com.quimia.quimiawebcrud.service.ShelfService;
import com.quimia.quimiawebcrud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/histories")
@RequiredArgsConstructor
public class HistoryController extends GenericController<History, Integer, HistoryResponseDTO, HistoryRequestDTO> {

    private final HistoryService historyService;
    private final UserService userService;
    private final ShelfService shelfService;
    private final HistoryTypeService historyTypeService;

    @Override
    protected CrudService<History, Integer, HistoryRequestDTO, HistoryResponseDTO> getService() {
        return historyService;
    }

    @Override
    protected String getBasePath() {
        return "/histories";
    }

    @Override
    protected String getViewFolder() {
        return "history";
    }

    @Override
    protected String getSingularName() {
        return "history";
    }

    @Override
    protected String getPluralName() {
        return "histories";
    }

    @Override
    protected HistoryResponseDTO newResponse() {
        return new HistoryResponseDTO();
    }

    @Override
    protected void addFormAttributes(Model model) {
        model.addAttribute("users", userService.readAll());
        model.addAttribute("shelves", shelfService.readAll());
        model.addAttribute("historyTypes", historyTypeService.readAll());
    }
}
