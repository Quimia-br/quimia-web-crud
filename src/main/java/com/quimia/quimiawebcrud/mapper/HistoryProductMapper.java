package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.HistoryProductRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryProductResponseDTO;
import com.quimia.quimiawebcrud.model.HistoryProduct;
import com.quimia.quimiawebcrud.model.UserProduct;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.References.*;

@Component
public class HistoryProductMapper implements Mapper<HistoryProduct, HistoryProductRequestDTO, HistoryProductResponseDTO> {

    @Override
    public HistoryProduct toEntity(HistoryProductRequestDTO request) {
        if (request == null) {
            return null;
        }
        return HistoryProduct.builder()
                .history(history(request.getHistoryId()))
                .userProduct(userProduct(request.getUserId(), request.getProductId()))
                .build();
    }

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

    @Override
    public void updateEntity(HistoryProduct entity, HistoryProductRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setHistory(history(request.getHistoryId()));
        entity.setUserProduct(userProduct(request.getUserId(), request.getProductId()));
    }
}
