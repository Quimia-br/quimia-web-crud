package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.UserProductRequestDTO;
import com.quimia.quimiawebcrud.dto.UserProductResponseDTO;
import com.quimia.quimiawebcrud.model.UserProduct;
import com.quimia.quimiawebcrud.model.id.UserProductId;
import org.springframework.stereotype.Component;

@Component
public class UserProductMapper implements Mapper<UserProduct, UserProductRequestDTO, UserProductResponseDTO> {

    @Override
    public UserProduct toEntity(UserProductRequestDTO request) {
        if (request == null) {
            return null;
        }
        return UserProduct.builder()
                .id(new UserProductId(request.getUserId(), request.getProductId()))
                .build();
    }

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

    @Override
    public void updateEntity(UserProduct entity, UserProductRequestDTO request) {
        throw new UnsupportedOperationException(
                "produtos_usuarios nao possui colunas alem da chave; exclua e crie novamente.");
    }
}
