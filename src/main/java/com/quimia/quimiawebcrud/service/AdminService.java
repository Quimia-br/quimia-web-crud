package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.AdminRequestDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.mapper.AdminMapper;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mindrot.jbcrypt.BCrypt;

@Service
@RequiredArgsConstructor
public class AdminService extends CrudService<Admin, Integer, AdminRequestDTO, AdminResponseDTO> {

    private final AdminRepository repository;
    private final AdminMapper mapper;

    @Override
    protected AdminRepository getRepository() {
        return repository;
    }

    @Override
    protected AdminMapper getMapper() {
        return mapper;
    }

    @Override
    @Transactional
    public AdminResponseDTO create(AdminRequestDTO request) {
        if (!hasText(request.getPassword())) {
            throw new IllegalArgumentException("A senha e obrigatoria.");
        }
        Admin entity = mapper.toEntity(request);
        entity.setPasswordHash(hashPassword(request.getPassword()));
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public AdminResponseDTO update(Integer id, AdminRequestDTO request) {
        Admin entity = getEntity(id);
        mapper.updateEntity(entity, request);
        if (hasText(request.getPassword())) {
            entity.setPasswordHash(hashPassword(request.getPassword()));
        }
        return mapper.toResponse(entity);
    }

    private String hashPassword(String originalPassword) {
        return BCrypt.hashpw(originalPassword, BCrypt.gensalt());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
