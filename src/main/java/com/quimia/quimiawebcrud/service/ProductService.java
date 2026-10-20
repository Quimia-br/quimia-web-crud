package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.ProductRequestDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.mapper.ProductMapper;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService extends CrudService<Product, Integer, ProductRequestDTO, ProductResponseDTO> {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Override
    protected ProductRepository getRepository() {
        return repository;
    }

    @Override
    protected ProductMapper getMapper() {
        return mapper;
    }
}
