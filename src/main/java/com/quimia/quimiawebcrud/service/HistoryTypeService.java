package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.HistoryTypeRequestDTO;
import com.quimia.quimiawebcrud.dto.HistoryTypeResponseDTO;
import com.quimia.quimiawebcrud.exception.DuplicateResourceException;
import com.quimia.quimiawebcrud.mapper.HistoryTypeMapper;
import com.quimia.quimiawebcrud.model.HistoryType;
import com.quimia.quimiawebcrud.repository.HistoryTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de tipos de histórico: validação, gravação, remoção e registro no log de edições.
 */
@Service
@RequiredArgsConstructor
public class HistoryTypeService extends CrudService<HistoryType, Integer, HistoryTypeRequestDTO, HistoryTypeResponseDTO> {

    private final HistoryTypeRepository repository;
    private final HistoryTypeMapper mapper;
    private final RecordRemovalService removal;
    private final EditLog editLog;

    @Override
    protected HistoryTypeRepository getRepository() {
        return repository;
    }

    @Override
    protected HistoryTypeMapper getMapper() {
        return mapper;
    }

    @Override
    protected EditLog getEditLog() {
        return editLog;
    }

    @Override
    protected void validate(HistoryTypeRequestDTO request, Integer id) {
        boolean taken = id == null
                ? repository.existsByNameIgnoreCase(request.getName())
                : repository.existsByNameIgnoreCaseAndIdNot(request.getName(), id);
        if (taken) {
            throw new DuplicateResourceException("Já existe um tipo de histórico com este nome.");
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        removal.removeHistoryType(getEntity(id));
    }
}
