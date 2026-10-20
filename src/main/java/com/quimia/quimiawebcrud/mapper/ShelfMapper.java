package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.ShelfRequestDTO;
import com.quimia.quimiawebcrud.dto.ShelfResponseDTO;
import com.quimia.quimiawebcrud.model.Shelf;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.References.idOf;
import static com.quimia.quimiawebcrud.mapper.References.user;

@Component
public class ShelfMapper implements Mapper<Shelf, ShelfRequestDTO, ShelfResponseDTO> {

    @Override
    public Shelf toEntity(ShelfRequestDTO request) {
        if (request == null) {
            return null;
        }
        return Shelf.builder()
                .user(user(request.getUserId()))
                .room(request.getRoom())
                .build();
    }

    @Override
    public ShelfResponseDTO toResponse(Shelf entity) {
        if (entity == null) {
            return null;
        }
        return ShelfResponseDTO.builder()
                .id(entity.getId())
                .userId(idOf(entity.getUser()))
                .room(entity.getRoom())
                .build();
    }

    @Override
    public void updateEntity(Shelf entity, ShelfRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUser(user(request.getUserId()));
        if (request.getRoom() != null && !request.getRoom().isBlank()) {
            entity.setRoom(request.getRoom());
        }
    }
}
