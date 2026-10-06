package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.HistoryTypeRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryTypeResponseDTO;
import com.quimia.quimiawebcrud.model.HistoryType;
import org.springframework.stereotype.Component;

@Component
public class HistoryTypeMapper implements Mapper<HistoryType, HistoryTypeRequestDTO, HistoryTypeResponseDTO> {

    @Override
    public HistoryType toEntity(HistoryTypeRequestDTO request) {
        if (request == null) {
            return null;
        }
        return HistoryType.builder()
                .name(request.getName())
                .build();
    }

    @Override
    public HistoryTypeResponseDTO toResponse(HistoryType entity) {
        if (entity == null) {
            return null;
        }
        return HistoryTypeResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    @Override
    public void updateEntity(HistoryType entity, HistoryTypeRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setName(request.getName());
    }
}
