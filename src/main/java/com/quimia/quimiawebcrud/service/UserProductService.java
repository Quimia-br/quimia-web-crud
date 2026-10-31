package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.UserProductResponseDTO;
import com.quimia.quimiawebcrud.exception.ResourceNotFoundException;
import com.quimia.quimiawebcrud.mapper.UserProductMapper;
import com.quimia.quimiawebcrud.model.UserProduct;
import com.quimia.quimiawebcrud.model.id.UserProductId;
import com.quimia.quimiawebcrud.repository.UserProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Regras de produtos de usuários: somente leitura.
 */
@Service
@RequiredArgsConstructor
public class UserProductService extends ReadService<UserProduct, UserProductId, UserProductResponseDTO> {

    private final UserProductRepository repository;
    private final UserProductMapper mapper;

    @Override
    protected UserProductRepository getRepository() {
        return repository;
    }

    @Override
    protected UserProductMapper getMapper() {
        return mapper;
    }

    @Override
    protected ResourceNotFoundException notFound(UserProductId id) {
        return new ResourceNotFoundException("Registro não encontrado (usuário "
                + id.getUserId() + ", produto " + id.getProductId() + ")");
    }
}
