package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Consulta somente leitura do histórico de alterações feitas pelos administradores.
 */
@Controller
@RequestMapping("/admin-logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("adminLogs", adminLogService.readAllNewestFirst());
        return "admin-log/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("adminLog", adminLogService.findById(id));
        return "admin-log/detail";
    }
}
