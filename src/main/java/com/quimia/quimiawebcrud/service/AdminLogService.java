package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.AdminLogRequestDTO;
import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.mapper.AdminLogMapper;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminLogService extends CrudService<AdminLog, Integer, AdminLogRequestDTO, AdminLogResponseDTO> {

    private final AdminLogRepository repository;
    private final AdminLogMapper mapper;

    @Override
    protected AdminLogRepository getRepository() {
        return repository;
    }

    @Override
    protected AdminLogMapper getMapper() {
        return mapper;
    }
}
