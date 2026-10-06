package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.LocationRequestDTO;
import com.quimia.quimiawebcrud.dto.LocationResponseDTO;
import com.quimia.quimiawebcrud.model.Location;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.References.idOf;
import static com.quimia.quimiawebcrud.mapper.References.user;

@Component
public class LocationMapper implements Mapper<Location, LocationRequestDTO, LocationResponseDTO> {

    @Override
    public Location toEntity(LocationRequestDTO request) {
        if (request == null) {
            return null;
        }
        return Location.builder()
                .user(user(request.getUserId()))
                .zipCode(request.getZipCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
    }

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

    @Override
    public void updateEntity(Location entity, LocationRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUser(user(request.getUserId()));
        entity.setZipCode(request.getZipCode());
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
    }
}
