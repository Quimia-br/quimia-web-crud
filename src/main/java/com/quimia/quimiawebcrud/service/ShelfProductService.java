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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShelfProductService {

    private final ShelfProductRepository repository;
    private final ShelfProductMapper mapper;

    public List<ShelfProductResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ShelfProductResponseDTO findById(Integer shelfId, Integer productId) {
        return mapper.toResponse(getEntity(shelfId, productId));
    }

    @Transactional
    public ShelfProductResponseDTO create(ShelfProductRequestDTO request) {
        ShelfProductId id = new ShelfProductId(request.getShelfId(), request.getProductId());
        if (repository.existsById(id)) {
            throw new IllegalStateException("O produto " + id.getProductId()
                    + " ja esta na estante " + id.getShelfId() + ".");
        }
        ShelfProduct entity = repository.save(mapper.toEntity(request));
        return mapper.toResponse(entity);
    }

    @Transactional
    public ShelfProductResponseDTO update(Integer shelfId, Integer productId, ShelfProductRequestDTO request) {
        ShelfProduct entity = getEntity(shelfId, productId);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer shelfId, Integer productId) {
        repository.delete(getEntity(shelfId, productId));
    }

    private ShelfProduct getEntity(Integer shelfId, Integer productId) {
        return repository.findById(new ShelfProductId(shelfId, productId))
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado (estante "
                        + shelfId + ", produto " + productId + ")"));
    }
}
