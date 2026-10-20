package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.ShelfService;
import com.quimia.quimiawebcrud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Consulta somente leitura das estantes cadastradas pelos usuários no aplicativo.
 */
@Controller
@RequestMapping("/shelves")
@RequiredArgsConstructor
public class ShelfController {

    private final ShelfService shelfService;
    private final UserService userService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("shelves", shelfService.readAll());
        model.addAttribute("usersById", Lookups.byId(userService.readAll()));
        return "shelf/list";
    }
}
