package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.HistoryService;
import com.quimia.quimiawebcrud.service.HistoryTypeService;
import com.quimia.quimiawebcrud.service.ShelfService;
import com.quimia.quimiawebcrud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Consulta somente leitura do histórico de uso registrado pelos usuários no aplicativo.
 */
@Controller
@RequestMapping("/histories")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;
    private final UserService userService;
    private final ShelfService shelfService;
    private final HistoryTypeService historyTypeService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("histories", historyService.readAll());
        model.addAttribute("usersById", Lookups.byId(userService.readAll()));
        model.addAttribute("shelvesById", Lookups.byId(shelfService.readAll()));
        model.addAttribute("historyTypesById", Lookups.byId(historyTypeService.readAll()));
        return "history/list";
    }
}
