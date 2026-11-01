package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.UserProductResponseDTO;
import com.quimia.quimiawebcrud.model.UserProduct;
import org.springframework.stereotype.Component;

/**
 * Converte produto de um usuário entre entidade e DTO.
 */
@Component
public class UserProductMapper implements ResponseMapper<UserProduct, UserProductResponseDTO> {

    @Override
    public UserProductResponseDTO toResponse(UserProduct entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return UserProductResponseDTO.builder()
                .userId(entity.getId().getUserId())
                .productId(entity.getId().getProductId())
                .build();
    }
}
