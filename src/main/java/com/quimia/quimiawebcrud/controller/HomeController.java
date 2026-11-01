package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.exception.BusinessException;
import com.quimia.quimiawebcrud.service.CurrentAdmin;
import com.quimia.quimiawebcrud.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

/**
 * Tela inicial do painel (dashboard).
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardService dashboardService;
    private final CurrentAdmin currentAdmin;
    private final MessageSource messages;

    /**
     * Tela inicial: monta o dashboard do período pedido (7, 30 ou 90 dias) com a saudação ao administrador logado.
     */
    @GetMapping("/")
    public String home(@RequestParam(name = "periodo", defaultValue = "7") int period, Locale locale, Model model) {
        String name = currentAdmin.require().getUserName();
        String firstName = name == null || name.isBlank() ? "administrador" : name.trim().split("\\s+")[0];
        model.addAttribute("dashboard", dashboardService.build(period, locale));
        model.addAttribute("adminName", firstName);
        model.addAttribute("adminInitial", firstName.substring(0, 1).toUpperCase());
        return "dashboard/index";
    }

    /**
     * Botão "Atualizar dashboard": recalcula a view e volta para o mesmo período. Se não der, o motivo aparece como
     * aviso na própria tela.
     */
    @PostMapping("/dashboard/refresh")
    public String refresh(@RequestParam(name = "periodo", defaultValue = "7") int period, Locale locale, RedirectAttributes redirect) {
        try {
            dashboardService.refresh(locale);
            redirect.addFlashAttribute("message", messages.getMessage("dash.refreshed", null, locale));
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/?periodo=" + DashboardService.normalizePeriod(period);
    }
}
