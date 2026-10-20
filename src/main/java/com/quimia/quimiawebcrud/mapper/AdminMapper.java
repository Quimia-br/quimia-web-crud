package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.AdminRequestDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.model.Admin;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper implements Mapper<Admin, AdminRequestDTO, AdminResponseDTO> {

    @Override
    public Admin toEntity(AdminRequestDTO request) {
        if (request == null) {
            return null;
        }
        return Admin.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .status(request.getStatus())
                .build();
    }

    @Override
    public AdminResponseDTO toResponse(Admin entity) {
        if (entity == null) {
            return null;
        }
        return AdminResponseDTO.builder()
                .id(entity.getId())
                .userName(entity.getUserName())
                .email(entity.getEmail())
                .signUpDate(entity.getSignUpDate())
                .status(entity.getStatus())
                .build();
    }

    @Override
    public void updateEntity(Admin entity, AdminRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUserName(request.getUserName());
        entity.setEmail(request.getEmail());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }
}
