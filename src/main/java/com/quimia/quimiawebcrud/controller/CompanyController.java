package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.CompanyRequestDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.model.enums.CompanySize;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.service.CompanyService;
import com.quimia.quimiawebcrud.service.CrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Telas de empresas em {@code /companies}: lista, detalhe, cadastro, edição e remoção.
 */
@Controller
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController extends GenericController<Company, Integer, CompanyRequestDTO, CompanyResponseDTO> {

    private final CompanyService companyService;

    @Override
    protected CrudService<Company, Integer, CompanyRequestDTO, CompanyResponseDTO> getService() {
        return companyService;
    }

    @Override
    protected String getBasePath() {
        return "/companies";
    }

    @Override
    protected String getViewFolder() {
        return "company";
    }

    @Override
    protected String getPluralName() {
        return "companies";
    }

    @Override
    protected List<ColumnField> getColumnFields() {
        return List.of(
                ColumnField.filter("nome", "Nome"),
                ColumnField.filter("cnpj", "CNPJ"),
                ColumnField.filter("email", "E-mail"),
                ColumnField.editable("porte", "Porte (" + ColumnField.options(CompanySize.values()) + ")"),
                ColumnField.editable("status", "Status (" + ColumnField.options(Status.values()) + ")"));
    }

    @Override
    protected Predicate<CompanyResponseDTO> filterBy(Map<String, String> params, Model model) {
        return company -> Filters.matchesText(params.get("busca"), company.getName(), company.getCnpj(), company.getEmail())
                && Filters.sameEnum(company.getSize(), params.get("porte"))
                && Filters.sameEnum(company.getStatus(), params.get("status"));
    }
}
