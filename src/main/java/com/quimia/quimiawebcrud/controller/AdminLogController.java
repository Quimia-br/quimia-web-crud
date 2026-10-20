package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.service.AdminLogService;
import com.quimia.quimiawebcrud.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Consulta somente leitura do histórico de alterações feitas pelos administradores.
 */
@Controller
@RequestMapping("/admin-logs")
@RequiredArgsConstructor
public class AdminLogController extends ListController<AdminLogResponseDTO> {

    private final AdminLogService adminLogService;
    private final AdminService adminService;

    @Override
    protected List<AdminLogResponseDTO> readAll() {
        return adminLogService.readAllNewestFirst();
    }

    @Override
    protected String getBasePath() {
        return "/admin-logs";
    }

    @Override
    protected String getViewFolder() {
        return "admin-log";
    }

    @Override
    protected String getPluralName() {
        return "adminLogs";
    }

    @Override
    protected void addListAttributes(Model model) {
        model.addAttribute("adminsById", Lookups.byId(adminService.readAll()));
    }

    @Override
    protected Predicate<AdminLogResponseDTO> filterBy(Map<String, String> params, Model model) {
        Map<Integer, AdminResponseDTO> adminsById = Lookups.fromModel(model, "adminsById");
        return log -> {
            AdminResponseDTO admin = adminsById.get(log.getAdminId());
            return Filters.matchesText(params.get("busca"), admin == null ? null : admin.getUserName())
                    && Filters.sameEnum(log.getAction(), params.get("acao"))
                    && Filters.sameText(log.getAffectedTable(), params.get("tabela"))
                    && Filters.inDateRange(log.getEditDate(), params.get("dataDe"), params.get("dataAte"));
        };
    }
}
