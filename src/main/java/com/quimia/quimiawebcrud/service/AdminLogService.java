package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.AdminLogRequestDTO;
import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.mapper.AdminLogMapper;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLogService {

    private final AdminLogRepository repository;
    private final AdminLogMapper mapper;

    public List<AdminLogResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public AdminLogResponseDTO findById(Integer id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public AdminLogResponseDTO create(AdminLogRequestDTO request) {
        AdminLog entity = repository.save(mapper.toEntity(request));
        return mapper.toResponse(entity);
    }

    @Transactional
    public AdminLogResponseDTO update(Integer id, AdminLogRequestDTO request) {
        AdminLog entity = getEntity(id);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(getEntity(id));
    }

    private AdminLog getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado com o ID: " + id));
    }
}
