package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.LocationRequestDTO;
import com.quimia.quimiawebcrud.dto.LocationResponseDTO;
import com.quimia.quimiawebcrud.mapper.LocationMapper;
import com.quimia.quimiawebcrud.model.Location;
import com.quimia.quimiawebcrud.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository repository;
    private final LocationMapper mapper;

    public List<LocationResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public LocationResponseDTO findById(Integer id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public LocationResponseDTO create(LocationRequestDTO request) {
        Location entity = repository.save(mapper.toEntity(request));
        return mapper.toResponse(entity);
    }

    @Transactional
    public LocationResponseDTO update(Integer id, LocationRequestDTO request) {
        Location entity = getEntity(id);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(getEntity(id));
    }

    private Location getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado com o ID: " + id));
    }
}
