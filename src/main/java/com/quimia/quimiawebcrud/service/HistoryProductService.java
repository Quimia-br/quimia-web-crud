package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.HistoryProductResponseDTO;
import com.quimia.quimiawebcrud.mapper.HistoryProductMapper;
import com.quimia.quimiawebcrud.model.HistoryProduct;
import com.quimia.quimiawebcrud.repository.HistoryProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Regras de produtos de históricos: somente leitura.
 */
@Service
@RequiredArgsConstructor
public class HistoryProductService extends ReadService<HistoryProduct, Integer, HistoryProductResponseDTO> {

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
