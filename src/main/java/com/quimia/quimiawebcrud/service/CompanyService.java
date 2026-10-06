package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.CompanyRequestDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.mapper.CompanyMapper;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository repository;
    private final CompanyMapper mapper;

    private String hashPassword(String originalPassword) {
        return BCrypt.hashpw(originalPassword, BCrypt.gensalt());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public List<CompanyResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CompanyResponseDTO findById(Integer id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public CompanyResponseDTO create(CompanyRequestDTO request) {
        if (!hasText(request.getPassword())) {
            throw new IllegalArgumentException("A senha e obrigatoria.");
        }
        Company entity = mapper.toEntity(request);
        entity.setPasswordHash(hashPassword(request.getPassword()));
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public CompanyResponseDTO update(Integer id, CompanyRequestDTO request) {
        Company entity = getEntity(id);
        mapper.updateEntity(entity, request);
        if (hasText(request.getPassword())) {
            entity.setPasswordHash(hashPassword(request.getPassword()));
        }
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(getEntity(id));
    }

    private Company getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado com o ID: " + id));
    }
}
