package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.DisposalRequestDTO;
import com.quimia.quimiawebcrud.dto.DisposalResponseDTO;
import com.quimia.quimiawebcrud.mapper.DisposalMapper;
import com.quimia.quimiawebcrud.model.Disposal;
import com.quimia.quimiawebcrud.repository.DisposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DisposalService extends CrudService<Disposal, Integer, DisposalRequestDTO, DisposalResponseDTO> {

    private final DisposalRepository repository;
    private final DisposalMapper mapper;

    @Override
    protected DisposalRepository getRepository() {
        return repository;
    }

    @Override
    protected DisposalMapper getMapper() {
        return mapper;
    }
}
