package com.quimia.quimiawebcrud.service.crud;

import com.quimia.quimiawebcrud.dto.response.HistoryResponseDTO;
import com.quimia.quimiawebcrud.mapper.HistoryMapper;
import com.quimia.quimiawebcrud.model.History;
import com.quimia.quimiawebcrud.repository.HistoryRepository;
import com.quimia.quimiawebcrud.service.ReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Regras de históricos: somente leitura.
 */
@Service
@RequiredArgsConstructor
public class HistoryService extends ReadService<History, Integer, HistoryResponseDTO> {

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
