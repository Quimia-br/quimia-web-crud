package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.ProductRequestDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.mapper.ProductMapper;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    public List<ProductResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ProductResponseDTO findById(Integer id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public ProductResponseDTO create(ProductRequestDTO request) {
        Product entity = repository.save(mapper.toEntity(request));
        return mapper.toResponse(entity);
    }

    @Transactional
    public ProductResponseDTO update(Integer id, ProductRequestDTO request) {
        Product entity = getEntity(id);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(getEntity(id));
    }

    private Product getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado com o ID: " + id));
    }
}
