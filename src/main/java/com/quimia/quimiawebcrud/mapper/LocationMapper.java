package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.LocationResponseDTO;
import com.quimia.quimiawebcrud.model.Location;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.References.idOf;

@Component
public class LocationMapper implements ResponseMapper<Location, LocationResponseDTO> {

    @Override
    public LocationResponseDTO toResponse(Location entity) {
        if (entity == null) {
            return null;
        }
        return LocationResponseDTO.builder()
                .id(entity.getId())
                .userId(idOf(entity.getUser()))
                .zipCode(entity.getZipCode())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }
}
