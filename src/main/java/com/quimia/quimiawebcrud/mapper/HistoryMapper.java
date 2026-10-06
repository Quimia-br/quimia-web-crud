package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.HistoryRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryResponseDTO;
import com.quimia.quimiawebcrud.model.History;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.References.*;

@Component
public class HistoryMapper implements Mapper<History, HistoryRequestDTO, HistoryResponseDTO> {

    @Override
    public History toEntity(HistoryRequestDTO request) {
        if (request == null) {
            return null;
        }
        return History.builder()
                .shelf(shelf(request.getShelfId()))
                .user(user(request.getUserId()))
                .historyType(historyType(request.getHistoryTypeId()))
                .executionDate(request.getExecutionDate())
                .resultDescription(request.getResultDescription())
                .build();
    }

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
                .build();
    }

    @Override
    public void updateEntity(History entity, HistoryRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setShelf(shelf(request.getShelfId()));
        entity.setUser(user(request.getUserId()));
        entity.setHistoryType(historyType(request.getHistoryTypeId()));
        entity.setResultDescription(request.getResultDescription());
        if (request.getExecutionDate() != null) {
            entity.setExecutionDate(request.getExecutionDate());
        }
    }
}
