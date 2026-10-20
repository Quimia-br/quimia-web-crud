package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.CompanyRequestDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.model.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper implements Mapper<Company, CompanyRequestDTO, CompanyResponseDTO> {

    @Override
    public Company toEntity(CompanyRequestDTO request) {
        if (request == null) {
            return null;
        }
        return Company.builder()
                .cnpj(request.getCnpj())
                .name(request.getName())
                .email(request.getEmail())
                .size(request.getSize())
                .status(request.getStatus())
                .photoUrl(request.getPhotoUrl())
                .build();
    }

    @Override
    public CompanyResponseDTO toResponse(Company entity) {
        if (entity == null) {
            return null;
        }
        return CompanyResponseDTO.builder()
                .id(entity.getId())
                .cnpj(entity.getCnpj())
                .name(entity.getName())
                .email(entity.getEmail())
                .size(entity.getSize())
                .signUpDate(entity.getSignUpDate())
                .status(entity.getStatus())
                .photoUrl(entity.getPhotoUrl())
                .build();
    }

    @Override
    public void updateEntity(Company entity, CompanyRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setCnpj(request.getCnpj());
        entity.setName(request.getName());
        entity.setEmail(request.getEmail());
        entity.setPhotoUrl(request.getPhotoUrl());
        if (request.getSize() != null) {
            entity.setSize(request.getSize());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }
}
