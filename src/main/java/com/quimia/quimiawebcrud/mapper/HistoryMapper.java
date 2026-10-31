package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.HistoryResponseDTO;
import com.quimia.quimiawebcrud.model.History;
import com.quimia.quimiawebcrud.model.Product;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.quimia.quimiawebcrud.mapper.References.idOf;

/**
 * Converte histórico entre entidade e DTO.
 */
@Component
public class HistoryMapper implements ResponseMapper<History, HistoryResponseDTO> {

    @Override
    public HistoryResponseDTO toResponse(History entity) {
        if (entity == null) {
            return null;
        }
        return HistoryResponseDTO.builder()
                .id(entity.getId())
                .shelfId(idOf(entity.getShelf()))
                .userId(idOf(entity.getUser()))
                .historyTypeId(idOf(entity.getHistoryType()))
                .executionDate(entity.getExecutionDate())
                .resultDescription(entity.getResultDescription())
                .productNames(entity.getProducts() == null ? List.of()
                        : entity.getProducts().stream().map(Product::getName).toList())
                .build();
    }
}
