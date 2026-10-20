package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.HistoryProductRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryProductResponseDTO;
import com.quimia.quimiawebcrud.mapper.HistoryProductMapper;
import com.quimia.quimiawebcrud.model.HistoryProduct;
import com.quimia.quimiawebcrud.repository.HistoryProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HistoryProductService extends CrudService<HistoryProduct, Integer, HistoryProductRequestDTO, HistoryProductResponseDTO> {

    private final HistoryProductRepository repository;
    private final HistoryProductMapper mapper;

    @Override
    protected HistoryProductRepository getRepository() {
        return repository;
    }

    @Override
    protected HistoryProductMapper getMapper() {
        return mapper;
    }
}
