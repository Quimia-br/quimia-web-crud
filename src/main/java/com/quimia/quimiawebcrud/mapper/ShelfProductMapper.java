package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.ShelfProductRequestDTO;
import com.quimia.quimiawebcrud.dto.ShelfProductResponseDTO;
import com.quimia.quimiawebcrud.model.ShelfProduct;
import com.quimia.quimiawebcrud.model.id.ShelfProductId;
import org.springframework.stereotype.Component;

@Component
public class ShelfProductMapper implements Mapper<ShelfProduct, ShelfProductRequestDTO, ShelfProductResponseDTO> {

    @Override
    public ShelfProduct toEntity(ShelfProductRequestDTO request) {
        if (request == null) {
            return null;
        }
        return ShelfProduct.builder()
                .id(new ShelfProductId(request.getShelfId(), request.getProductId()))
                .addedAt(request.getAddedAt())
                .build();
    }

    @Override
    public ShelfProductResponseDTO toResponse(ShelfProduct entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return ShelfProductResponseDTO.builder()
                .shelfId(entity.getId().getShelfId())
                .productId(entity.getId().getProductId())
                .addedAt(entity.getAddedAt())
                .build();
    }

    @Override
    public void updateEntity(ShelfProduct entity, ShelfProductRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        if (request.getAddedAt() != null) {
            entity.setAddedAt(request.getAddedAt());
        }
    }
}
