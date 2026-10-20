package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.LocationRequestDTO;
import com.quimia.quimiawebcrud.dto.LocationResponseDTO;
import com.quimia.quimiawebcrud.mapper.LocationMapper;
import com.quimia.quimiawebcrud.model.Location;
import com.quimia.quimiawebcrud.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService extends CrudService<Location, Integer, LocationRequestDTO, LocationResponseDTO> {

    private final LocationRepository repository;
    private final LocationMapper mapper;

    @Override
    protected LocationRepository getRepository() {
        return repository;
    }

    @Override
    protected LocationMapper getMapper() {
        return mapper;
    }
}
