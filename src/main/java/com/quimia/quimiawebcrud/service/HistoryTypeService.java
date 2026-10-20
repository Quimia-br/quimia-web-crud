package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.HistoryTypeRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryTypeResponseDTO;
import com.quimia.quimiawebcrud.mapper.HistoryTypeMapper;
import com.quimia.quimiawebcrud.model.HistoryType;
import com.quimia.quimiawebcrud.repository.HistoryTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HistoryTypeService extends CrudService<HistoryType, Integer, HistoryTypeRequestDTO, HistoryTypeResponseDTO> {

    private final HistoryTypeRepository repository;
    private final HistoryTypeMapper mapper;

    @Override
    protected HistoryTypeRepository getRepository() {
        return repository;
    }

    @Override
    protected HistoryTypeMapper getMapper() {
        return mapper;
    }
}
