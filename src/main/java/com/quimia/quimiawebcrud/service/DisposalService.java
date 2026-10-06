package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.DisposalRequestDTO;
import com.quimia.quimiawebcrud.dto.DisposalResponseDTO;
import com.quimia.quimiawebcrud.mapper.DisposalMapper;
import com.quimia.quimiawebcrud.model.Disposal;
import com.quimia.quimiawebcrud.repository.DisposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisposalService {

    private final DisposalRepository repository;
    private final DisposalMapper mapper;

    public List<DisposalResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public DisposalResponseDTO findById(Integer id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public DisposalResponseDTO create(DisposalRequestDTO request) {
        Disposal entity = repository.save(mapper.toEntity(request));
        return mapper.toResponse(entity);
    }

    @Transactional
    public DisposalResponseDTO update(Integer id, DisposalRequestDTO request) {
        Disposal entity = getEntity(id);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(getEntity(id));
    }

    private Disposal getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado com o ID: " + id));
    }
}
