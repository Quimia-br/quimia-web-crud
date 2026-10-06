package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.HistoryProductRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryProductResponseDTO;
import com.quimia.quimiawebcrud.mapper.HistoryProductMapper;
import com.quimia.quimiawebcrud.model.HistoryProduct;
import com.quimia.quimiawebcrud.repository.HistoryProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryProductService {

    private final HistoryProductRepository repository;
    private final HistoryProductMapper mapper;

    public List<HistoryProductResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public HistoryProductResponseDTO findById(Integer id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public HistoryProductResponseDTO create(HistoryProductRequestDTO request) {
        HistoryProduct entity = repository.save(mapper.toEntity(request));
        return mapper.toResponse(entity);
    }

    @Transactional
    public HistoryProductResponseDTO update(Integer id, HistoryProductRequestDTO request) {
        HistoryProduct entity = getEntity(id);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(getEntity(id));
    }

    private HistoryProduct getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado com o ID: " + id));
    }
}
