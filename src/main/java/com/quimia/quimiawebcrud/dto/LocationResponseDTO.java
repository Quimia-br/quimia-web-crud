package com.quimia.quimiawebcrud.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LocationResponseDTO extends BaseResponseDTO {

    private Integer userId;
    private String zipCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
