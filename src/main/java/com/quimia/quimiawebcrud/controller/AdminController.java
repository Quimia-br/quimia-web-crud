package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.AdminRequestDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.service.AdminService;
import com.quimia.quimiawebcrud.service.CrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admins")
@RequiredArgsConstructor
public class AdminController extends GenericController<Admin, Integer, AdminResponseDTO, AdminRequestDTO> {

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
    protected String getSingularName() {
        return "admin";
    }

    @Override
    protected String getPluralName() {
        return "admins";
    }

    @Override
    protected AdminResponseDTO newResponse() {
        return new AdminResponseDTO();
    }
}
