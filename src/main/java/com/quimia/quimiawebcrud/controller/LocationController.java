package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.LocationService;
import com.quimia.quimiawebcrud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Consulta somente leitura das localizações informadas pelos usuários no aplicativo.
 */
@Controller
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final UserService userService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("locations", locationService.readAll());
        model.addAttribute("usersById", Lookups.byId(userService.readAll()));
        return "location/list";
    }
}
