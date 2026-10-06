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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProductService {

    private final UserProductRepository repository;
    private final UserProductMapper mapper;

    public List<UserProductResponseDTO> readAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public UserProductResponseDTO findById(Integer userId, Integer productId) {
        return mapper.toResponse(getEntity(userId, productId));
    }

    @Transactional
    public UserProductResponseDTO create(UserProductRequestDTO request) {
        UserProductId id = new UserProductId(request.getUserId(), request.getProductId());
        if (repository.existsById(id)) {
            throw new IllegalStateException("O usuario " + id.getUserId()
                    + " ja possui o produto " + id.getProductId() + ".");
        }
        UserProduct entity = repository.save(mapper.toEntity(request));
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer userId, Integer productId) {
        repository.delete(getEntity(userId, productId));
    }

    private UserProduct getEntity(Integer userId, Integer productId) {
        return repository.findById(new UserProductId(userId, productId))
                .orElseThrow(() -> new RuntimeException("Registro nao encontrado (usuario "
                        + userId + ", produto " + productId + ")"));
    }
}
