package com.quimia.quimiawebcrud.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisposalRequestDTO {

    private Integer productId;
    private String productDisposalMethod;
    private String packagingDisposalMethod;
    private String disposalRestrictions;
    private String environmentalPrecautions;
}
