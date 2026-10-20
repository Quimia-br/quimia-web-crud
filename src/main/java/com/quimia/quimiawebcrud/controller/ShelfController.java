package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.ShelfRequestDTO;
import com.quimia.quimiawebcrud.dto.ShelfResponseDTO;
import com.quimia.quimiawebcrud.model.Shelf;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.ShelfService;
import com.quimia.quimiawebcrud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shelves")
@RequiredArgsConstructor
public class ShelfController extends GenericController<Shelf, Integer, ShelfResponseDTO, ShelfRequestDTO> {

    private final ShelfService shelfService;
    private final UserService userService;

    @Override
    protected CrudService<Shelf, Integer, ShelfRequestDTO, ShelfResponseDTO> getService() {
        return shelfService;
    }

    @Override
    protected String getBasePath() {
        return "/shelves";
    }

    @Override
    protected String getViewFolder() {
        return "shelf";
    }

    @Override
    protected String getSingularName() {
        return "shelf";
    }

    @Override
    protected String getPluralName() {
        return "shelves";
    }

    @Override
    protected ShelfResponseDTO newResponse() {
        return new ShelfResponseDTO();
    }

    @Override
    protected void addFormAttributes(Model model) {
        model.addAttribute("users", userService.readAll());
    }
}
