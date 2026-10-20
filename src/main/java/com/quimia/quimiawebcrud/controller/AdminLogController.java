package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.AdminLogService;
import com.quimia.quimiawebcrud.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Consulta somente leitura do histórico de alterações feitas pelos administradores.
 */
@Controller
@RequestMapping("/admin-logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;
    private final AdminService adminService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("adminLogs", adminLogService.readAllNewestFirst());
        model.addAttribute("adminsById", Lookups.byId(adminService.readAll()));
        return "admin-log/list";
    }
}
