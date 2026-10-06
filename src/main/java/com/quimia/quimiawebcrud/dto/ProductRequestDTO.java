package com.quimia.quimiawebcrud.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDTO {

    private Integer companyId;
    private String casNumber;
    private String name;
    private String brand;
    private String instructions;
    private String dosage;
}
