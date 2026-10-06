package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.ShelfRequestDTO;
import com.quimia.quimiawebcrud.dto.ShelfResponseDTO;
import com.quimia.quimiawebcrud.mapper.ShelfMapper;
import com.quimia.quimiawebcrud.model.Shelf;
import com.quimia.quimiawebcrud.repository.ShelfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShelfService {

    private final ShelfRepository repository;
    private final ShelfMapper mapper;

    public List<ShelfResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ShelfResponseDTO findById(Integer id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public ShelfResponseDTO create(ShelfRequestDTO request) {
        Shelf entity = repository.save(mapper.toEntity(request));
        return mapper.toResponse(entity);
    }

    @Transactional
    public ShelfResponseDTO update(Integer id, ShelfRequestDTO request) {
        Shelf entity = getEntity(id);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(getEntity(id));
    }

    private Shelf getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado com o ID: " + id));
    }
}
