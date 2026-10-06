package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.DisposalRequestDTO;
import com.quimia.quimiawebcrud.dto.DisposalResponseDTO;
import com.quimia.quimiawebcrud.model.Disposal;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.References.idOf;
import static com.quimia.quimiawebcrud.mapper.References.product;

@Component
public class DisposalMapper implements Mapper<Disposal, DisposalRequestDTO, DisposalResponseDTO> {

    @Override
    public Disposal toEntity(DisposalRequestDTO request) {
        if (request == null) {
            return null;
        }
        return Disposal.builder()
                .product(product(request.getProductId()))
                .productDisposalMethod(request.getProductDisposalMethod())
                .packagingDisposalMethod(request.getPackagingDisposalMethod())
                .disposalRestrictions(request.getDisposalRestrictions())
                .environmentalPrecautions(request.getEnvironmentalPrecautions())
                .build();
    }

    @Override
    public DisposalResponseDTO toResponse(Disposal entity) {
        if (entity == null) {
            return null;
        }
        return DisposalResponseDTO.builder()
                .id(entity.getId())
                .productId(idOf(entity.getProduct()))
                .productDisposalMethod(entity.getProductDisposalMethod())
                .packagingDisposalMethod(entity.getPackagingDisposalMethod())
                .disposalRestrictions(entity.getDisposalRestrictions())
                .environmentalPrecautions(entity.getEnvironmentalPrecautions())
                .build();
    }

    @Override
    public void updateEntity(Disposal entity, DisposalRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setProduct(product(request.getProductId()));
        entity.setProductDisposalMethod(request.getProductDisposalMethod());
        entity.setPackagingDisposalMethod(request.getPackagingDisposalMethod());
        entity.setDisposalRestrictions(request.getDisposalRestrictions());
        entity.setEnvironmentalPrecautions(request.getEnvironmentalPrecautions());
    }
}
