package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.CompanyRequestDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.mapper.CompanyMapper;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mindrot.jbcrypt.BCrypt;

@Service
@RequiredArgsConstructor
public class CompanyService extends CrudService<Company, Integer, CompanyRequestDTO, CompanyResponseDTO> {

    private final CompanyRepository repository;
    private final CompanyMapper mapper;

    @Override
    protected CompanyRepository getRepository() {
        return repository;
    }

    @Override
    protected CompanyMapper getMapper() {
        return mapper;
    }

    @Override
    @Transactional
    public CompanyResponseDTO create(CompanyRequestDTO request) {
        if (!hasText(request.getPassword())) {
            throw new IllegalArgumentException("A senha e obrigatoria.");
        }
        Company entity = mapper.toEntity(request);
        entity.setPasswordHash(hashPassword(request.getPassword()));
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public CompanyResponseDTO update(Integer id, CompanyRequestDTO request) {
        Company entity = getEntity(id);
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
