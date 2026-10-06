package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.ProductRequestDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.model.Product;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.References.company;
import static com.quimia.quimiawebcrud.mapper.References.idOf;

@Component
public class ProductMapper implements Mapper<Product, ProductRequestDTO, ProductResponseDTO> {

    @Override
    public Product toEntity(ProductRequestDTO request) {
        if (request == null) {
            return null;
        }
        return Product.builder()
                .company(company(request.getCompanyId()))
                .casNumber(request.getCasNumber())
                .name(request.getName())
                .brand(request.getBrand())
                .instructions(request.getInstructions())
                .dosage(request.getDosage())
                .build();
    }

    @Override
    public ProductResponseDTO toResponse(Product entity) {
        if (entity == null) {
            return null;
        }
        return ProductResponseDTO.builder()
                .id(entity.getId())
                .companyId(idOf(entity.getCompany()))
                .casNumber(entity.getCasNumber())
                .name(entity.getName())
                .brand(entity.getBrand())
                .instructions(entity.getInstructions())
                .dosage(entity.getDosage())
                .build();
    }

    @Override
    public void updateEntity(Product entity, ProductRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setCompany(company(request.getCompanyId()));
        entity.setCasNumber(request.getCasNumber());
        entity.setName(request.getName());
        entity.setInstructions(request.getInstructions());
        entity.setDosage(request.getDosage());
        if (request.getBrand() != null && !request.getBrand().isBlank()) {
            entity.setBrand(request.getBrand());
        }
    }
}
