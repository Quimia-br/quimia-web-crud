package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.ProductRequestDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.exception.DuplicateResourceException;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.ProductMapper;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import com.quimia.quimiawebcrud.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService extends CrudService<Product, Integer, ProductRequestDTO, ProductResponseDTO> {

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final CompanyRepository companies;
    private final RecordRemovalService removal;
    private final EditLog editLog;

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
