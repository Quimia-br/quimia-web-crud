package com.quimia.quimiawebcrud.controller.crud;

import com.quimia.quimiawebcrud.controller.support.ColumnField;
import com.quimia.quimiawebcrud.controller.support.Filters;
import com.quimia.quimiawebcrud.controller.support.GenericController;
import com.quimia.quimiawebcrud.dto.request.AdminRequestDTO;
import com.quimia.quimiawebcrud.dto.response.AdminResponseDTO;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.crud.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Telas de administradores em {@code /admins}: lista, detalhe, cadastro, edição e remoção.
 */
@Controller
@RequestMapping("/admins")
@RequiredArgsConstructor
public class AdminController extends GenericController<Admin, Integer, AdminRequestDTO, AdminResponseDTO> {

    private final AdminService adminService;

    @Override
    protected CrudService<Admin, Integer, AdminRequestDTO, AdminResponseDTO> getService() {
        return adminService;
    }

    @Override
    protected String getBasePath() {
        return "/admins";
    }

    @Override
    protected String getViewFolder() {
        return "admin";
    }

    @Override
    protected String getPluralName() {
        return "admins";
    }

    @Override
    protected List<ColumnField> getColumnFields() {
        return List.of(
                ColumnField.filter("nome", "Nome"),
                ColumnField.filter("email", "E-mail"),
                ColumnField.editable("status", "Status (" + ColumnField.options(Status.values()) + ")"));
    }

    @Override
    protected Predicate<AdminResponseDTO> filterBy(Map<String, String> params, Model model) {
        return admin -> Filters.matchesText(params.get("busca"), admin.getUserName(), admin.getEmail())
                && Filters.sameEnum(admin.getStatus(), params.get("status"))
                && Filters.inDateRange(admin.getSignUpDate(), params.get("cadastroDe"), params.get("cadastroAte"));
    }
}
