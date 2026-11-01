package com.quimia.quimiawebcrud.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Dados de localização que as telas mostram.
 */
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
