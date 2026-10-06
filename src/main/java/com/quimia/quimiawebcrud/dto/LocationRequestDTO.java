package com.quimia.quimiawebcrud.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequestDTO {

    private Integer userId;
    private String zipCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
