package com.quimia.quimiawebcrud.service.crud;

import com.quimia.quimiawebcrud.dto.ProductRequestDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.exception.DuplicateResourceException;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.ProductMapper;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import com.quimia.quimiawebcrud.repository.ProductRepository;
import com.quimia.quimiawebcrud.service.CrudService;
import com.quimia.quimiawebcrud.service.EditLog;
import com.quimia.quimiawebcrud.service.removal.RecordRemovalService;
import com.quimia.quimiawebcrud.service.verification.ExternalChecks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de produtos: validação, gravação, remoção e registro no log de edições.
 */
@Service
@RequiredArgsConstructor
public class ProductService extends CrudService<Product, Integer, ProductRequestDTO, ProductResponseDTO> {

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final CompanyRepository companies;
    private final RecordRemovalService removal;
    private final EditLog editLog;
    private final ExternalChecks checks;

    @Override
    protected ProductRepository getRepository() {
        return repository;
    }

    @Override
    protected ProductMapper getMapper() {
        return mapper;
    }

    @Override
    protected EditLog getEditLog() {
        return editLog;
    }

    /** A substância do número CAS precisa existir no PubChem; só é conferida quando o número mudou. */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void verifyBeforeSave(ProductRequestDTO request, Integer id) {
        Product current = id == null ? null : repository.findById(id).orElse(null);
        checks.requireCas(request.getCasNumber(), current == null ? null : current.getCasNumber());
    }

    /** Todo produto pertence a uma empresa cadastrada, e o número CAS identifica um único produto. */
    @Override
    protected void validate(ProductRequestDTO request, Integer id) {
        if (!companies.existsById(request.getCompanyId())) {
            throw new InvalidDataException("A empresa selecionada não existe.");
        }
        boolean casTaken = id == null
                ? repository.existsByCasNumber(request.getCasNumber())
                : repository.existsByCasNumberAndIdNot(request.getCasNumber(), id);
        if (casTaken) {
            throw new DuplicateResourceException("Já existe um produto com este número CAS.");
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        removal.removeProduct(getEntity(id));
    }
}
