package com.quimia.quimiawebcrud.service.crud;

import com.quimia.quimiawebcrud.dto.response.LocationResponseDTO;
import com.quimia.quimiawebcrud.mapper.LocationMapper;
import com.quimia.quimiawebcrud.model.Location;
import com.quimia.quimiawebcrud.repository.LocationRepository;
import com.quimia.quimiawebcrud.service.ReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Regras de localizações: somente leitura.
 */
@Service
@RequiredArgsConstructor
public class LocationService extends ReadService<Location, Integer, LocationResponseDTO> {

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
