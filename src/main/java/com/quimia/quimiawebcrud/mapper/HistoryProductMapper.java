package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.HistoryProductResponseDTO;
import com.quimia.quimiawebcrud.model.HistoryProduct;
import com.quimia.quimiawebcrud.model.UserProduct;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.References.idOf;

/**
 * Converte produto de um histórico entre entidade e DTO.
 */
@Component
public class HistoryProductMapper implements ResponseMapper<HistoryProduct, HistoryProductResponseDTO> {

    @Override
    public HistoryProductResponseDTO toResponse(HistoryProduct entity) {
        if (entity == null) {
            return null;
        }
        UserProduct up = entity.getUserProduct();
        return HistoryProductResponseDTO.builder()
                .id(entity.getId())
                .historyId(idOf(entity.getHistory()))
                .userId(up != null && up.getId() != null ? up.getId().getUserId() : null)
                .productId(up != null && up.getId() != null ? up.getId().getProductId() : null)
                .build();
    }
}
