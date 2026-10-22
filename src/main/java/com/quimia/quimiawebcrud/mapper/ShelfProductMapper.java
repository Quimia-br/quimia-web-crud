package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.ShelfProductResponseDTO;
import com.quimia.quimiawebcrud.model.ShelfProduct;
import org.springframework.stereotype.Component;

@Component
public class ShelfProductMapper implements ResponseMapper<ShelfProduct, ShelfProductResponseDTO> {

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
}
