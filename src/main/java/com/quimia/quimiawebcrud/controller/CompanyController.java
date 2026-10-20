package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.CompanyRequestDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.service.CompanyService;
import com.quimia.quimiawebcrud.service.CrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController extends GenericController<Company, Integer, CompanyResponseDTO, CompanyRequestDTO> {

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
    protected String getSingularName() {
        return "company";
    }

    @Override
    protected String getPluralName() {
        return "companies";
    }

    @Override
    protected CompanyResponseDTO newResponse() {
        return new CompanyResponseDTO();
    }
}
