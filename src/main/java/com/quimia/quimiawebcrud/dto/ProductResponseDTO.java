package com.quimia.quimiawebcrud.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductResponseDTO extends BaseResponseDTO {

    private Integer companyId;
    private String casNumber;
    private String name;
    private String brand;
    private String instructions;
    private String dosage;
}
