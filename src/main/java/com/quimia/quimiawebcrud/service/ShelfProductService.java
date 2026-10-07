package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.ShelfProductRequestDTO;
import com.quimia.quimiawebcrud.dto.ShelfProductResponseDTO;
import com.quimia.quimiawebcrud.mapper.ShelfProductMapper;
import com.quimia.quimiawebcrud.model.ShelfProduct;
import com.quimia.quimiawebcrud.model.id.ShelfProductId;
import com.quimia.quimiawebcrud.repository.ShelfProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShelfProductService extends CrudService<ShelfProduct, ShelfProductId, ShelfProductRequestDTO, ShelfProductResponseDTO> {

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
    @Transactional
    public ShelfProductResponseDTO create(ShelfProductRequestDTO request) {
        ShelfProductId id = new ShelfProductId(request.getShelfId(), request.getProductId());
        if (repository.existsById(id)) {
            throw new IllegalStateException("O produto " + id.getProductId()
                    + " ja esta na estante " + id.getShelfId() + ".");
        }
        return super.create(request);
    }

    @Override
    protected RuntimeException notFound(ShelfProductId id) {
        return new RuntimeException("Registro nao encontrado (estante "
                + id.getShelfId() + ", produto " + id.getProductId() + ")");
    }

    public ShelfProductResponseDTO findById(Integer shelfId, Integer productId) {
        return findById(new ShelfProductId(shelfId, productId));
    }

    public ShelfProductResponseDTO update(Integer shelfId, Integer productId, ShelfProductRequestDTO request) {
        return update(new ShelfProductId(shelfId, productId), request);
    }

    public void delete(Integer shelfId, Integer productId) {
        delete(new ShelfProductId(shelfId, productId));
    }
}
