package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.ShelfResponseDTO;
import com.quimia.quimiawebcrud.model.Shelf;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.References.idOf;

@Component
public class ShelfMapper implements ResponseMapper<Shelf, ShelfResponseDTO> {

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
}
