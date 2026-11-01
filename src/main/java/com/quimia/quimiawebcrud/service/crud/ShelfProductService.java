package com.quimia.quimiawebcrud.service.crud;

import com.quimia.quimiawebcrud.dto.response.ShelfProductResponseDTO;
import com.quimia.quimiawebcrud.exception.ResourceNotFoundException;
import com.quimia.quimiawebcrud.mapper.ShelfProductMapper;
import com.quimia.quimiawebcrud.model.ShelfProduct;
import com.quimia.quimiawebcrud.model.id.ShelfProductId;
import com.quimia.quimiawebcrud.repository.ShelfProductRepository;
import com.quimia.quimiawebcrud.service.ReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Regras de produtos de estantes: somente leitura.
 */
@Service
@RequiredArgsConstructor
public class ShelfProductService extends ReadService<ShelfProduct, ShelfProductId, ShelfProductResponseDTO> {

    private final ShelfProductRepository repository;
    private final ShelfProductMapper mapper;

    @Override
    protected ShelfProductRepository getRepository() {
        return repository;
    }

    @Override
    protected ShelfProductMapper getMapper() {
        return mapper;
    }

    @Override
    protected ResourceNotFoundException notFound(ShelfProductId id) {
        return new ResourceNotFoundException("Registro não encontrado (estante "
                + id.getShelfId() + ", produto " + id.getProductId() + ")");
    }
}
