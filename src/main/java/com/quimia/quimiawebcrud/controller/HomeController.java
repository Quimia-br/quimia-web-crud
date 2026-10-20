package com.quimia.quimiawebcrud.controller;

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

    @GetMapping("/")
    public String home(@RequestParam(name = "periodo", defaultValue = "7") int period, Model model) {
        model.addAttribute("dashboard", dashboardService.build(period));
        return "dashboard/index";
    }
}
