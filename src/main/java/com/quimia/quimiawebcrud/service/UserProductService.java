package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.UserProductRequestDTO;
import com.quimia.quimiawebcrud.dto.UserProductResponseDTO;
import com.quimia.quimiawebcrud.mapper.UserProductMapper;
import com.quimia.quimiawebcrud.model.UserProduct;
import com.quimia.quimiawebcrud.model.id.UserProductId;
import com.quimia.quimiawebcrud.repository.UserProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProductService extends CrudService<UserProduct, UserProductId, UserProductRequestDTO, UserProductResponseDTO> {

    private final UserProductRepository repository;
    private final UserProductMapper mapper;

    @Override
    protected UserProductRepository getRepository() {
        return repository;
    }

    @Override
    protected UserProductMapper getMapper() {
        return mapper;
    }

    @Override
    @Transactional
    public UserProductResponseDTO create(UserProductRequestDTO request) {
        UserProductId id = new UserProductId(request.getUserId(), request.getProductId());
        if (repository.existsById(id)) {
            throw new IllegalStateException("O usuario " + id.getUserId()
                    + " ja possui o produto " + id.getProductId() + ".");
        }
        return super.create(request);
    }

    @Override
    protected RuntimeException notFound(UserProductId id) {
        return new RuntimeException("Registro nao encontrado (usuario "
                + id.getUserId() + ", produto " + id.getProductId() + ")");
    }

    public UserProductResponseDTO findById(Integer userId, Integer productId) {
        return findById(new UserProductId(userId, productId));
    }

    public void delete(Integer userId, Integer productId) {
        delete(new UserProductId(userId, productId));
    }
}
