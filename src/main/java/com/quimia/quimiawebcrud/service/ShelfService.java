package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.ShelfRequestDTO;
import com.quimia.quimiawebcrud.dto.ShelfResponseDTO;
import com.quimia.quimiawebcrud.mapper.ShelfMapper;
import com.quimia.quimiawebcrud.model.Shelf;
import com.quimia.quimiawebcrud.repository.ShelfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShelfService extends CrudService<Shelf, Integer, ShelfRequestDTO, ShelfResponseDTO> {

    private final ShelfRepository repository;
    private final ShelfMapper mapper;

    @Override
    protected ShelfRepository getRepository() {
        return repository;
    }

    @Override
    protected ShelfMapper getMapper() {
        return mapper;
    }
}
