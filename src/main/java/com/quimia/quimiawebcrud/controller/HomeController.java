package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.CurrentAdmin;
import com.quimia.quimiawebcrud.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela inicial do painel (dashboard).
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardService dashboardService;
    private final CurrentAdmin currentAdmin;

    @GetMapping("/")
    public String home(@RequestParam(name = "periodo", defaultValue = "7") int period, Model model) {
        String name = currentAdmin.require().getUserName();
        String firstName = name == null || name.isBlank() ? "administrador" : name.trim().split("\\s+")[0];
        model.addAttribute("dashboard", dashboardService.build(period));
        model.addAttribute("adminName", firstName);
        model.addAttribute("adminInitial", firstName.substring(0, 1).toUpperCase());
        return "dashboard/index";
    }
}
