package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.DisposalRequestDTO;
import com.quimia.quimiawebcrud.dto.DisposalResponseDTO;
import com.quimia.quimiawebcrud.exception.DuplicateResourceException;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.DisposalMapper;
import com.quimia.quimiawebcrud.model.Disposal;
import com.quimia.quimiawebcrud.repository.DisposalRepository;
import com.quimia.quimiawebcrud.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de descartes: validação, gravação, remoção e registro no log de edições.
 */
@Service
@RequiredArgsConstructor
public class DisposalService extends CrudService<Disposal, Integer, DisposalRequestDTO, DisposalResponseDTO> {

    private final DisposalRepository repository;
    private final DisposalMapper mapper;
    private final ProductRepository products;
    private final RecordRemovalService removal;
    private final EditLog editLog;

    @Override
    protected DisposalRepository getRepository() {
        return repository;
    }

    @Override
    protected DisposalMapper getMapper() {
        return mapper;
    }

    @Override
    protected EditLog getEditLog() {
        return editLog;
    }

    /** Cada produto cadastrado tem no máximo uma ficha de descarte. */
    @Override
    protected void validate(DisposalRequestDTO request, Integer id) {
        if (!products.existsById(request.getProductId())) {
            throw new InvalidDataException("O produto selecionado não existe.");
        }
        boolean taken = id == null
                ? repository.existsByProductId(request.getProductId())
                : repository.existsByProductIdAndIdNot(request.getProductId(), id);
        if (taken) {
            throw new DuplicateResourceException("Este produto já tem uma ficha de descarte.");
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        removal.removeDisposal(getEntity(id));
    }
}
