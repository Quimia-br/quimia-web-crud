package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.HistoryRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryResponseDTO;
import com.quimia.quimiawebcrud.mapper.HistoryMapper;
import com.quimia.quimiawebcrud.model.History;
import com.quimia.quimiawebcrud.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HistoryService extends CrudService<History, Integer, HistoryRequestDTO, HistoryResponseDTO> {

    private final HistoryRepository repository;
    private final HistoryMapper mapper;

    @Override
    protected HistoryRepository getRepository() {
        return repository;
    }

    @Override
    protected HistoryMapper getMapper() {
        return mapper;
    }
}
