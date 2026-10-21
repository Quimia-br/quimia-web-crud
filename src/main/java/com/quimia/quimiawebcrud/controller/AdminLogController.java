package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.exception.BusinessException;
import com.quimia.quimiawebcrud.service.RecordRestoreService;
import com.quimia.quimiawebcrud.service.AdminLogService;
import com.quimia.quimiawebcrud.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Histórico de alterações feitas pelos administradores. Exclusões que guardaram uma cópia podem ser desfeitas
 * (restauradas) pela seta da linha.
 */
@Slf4j
@Controller
@RequestMapping("/admin-logs")
@RequiredArgsConstructor
public class AdminLogController extends ListController<AdminLogResponseDTO> {

    private final AdminLogService adminLogService;
    private final AdminService adminService;
    private final RecordRestoreService restoreService;

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
        model.addAttribute("restorableIds", adminLogService.restorableIds());
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

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable("id") Integer id, RedirectAttributes redirect) {
        try {
            restoreService.restore(id);
            redirect.addFlashAttribute("message", "Registro restaurado com sucesso.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Restauração bloqueada pelo banco no registro {} do log: {}", id, e.getMostSpecificCause().getMessage());
            redirect.addFlashAttribute("error", "Não foi possível restaurar: já existe um registro com os mesmos dados "
                    + "(por exemplo, o mesmo e-mail) ou algo de que ele depende foi apagado depois. Restaure antes o que falta.");
        }
        return "redirect:" + getBasePath();
    }
}
