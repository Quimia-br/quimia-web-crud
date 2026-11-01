package com.quimia.quimiawebcrud.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Dados de produto que as telas mostram.
 */
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
