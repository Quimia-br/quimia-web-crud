package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.HistoryTypeRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryTypeResponseDTO;
import com.quimia.quimiawebcrud.mapper.HistoryTypeMapper;
import com.quimia.quimiawebcrud.model.HistoryType;
import com.quimia.quimiawebcrud.repository.HistoryTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryTypeService {

    private final HistoryTypeRepository repository;
    private final HistoryTypeMapper mapper;

    public List<HistoryTypeResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public HistoryTypeResponseDTO findById(Integer id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public HistoryTypeResponseDTO create(HistoryTypeRequestDTO request) {
        HistoryType entity = repository.save(mapper.toEntity(request));
        return mapper.toResponse(entity);
    }

    @Transactional
    public HistoryTypeResponseDTO update(Integer id, HistoryTypeRequestDTO request) {
        HistoryType entity = getEntity(id);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(getEntity(id));
    }

    private HistoryType getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado com o ID: " + id));
    }
}
