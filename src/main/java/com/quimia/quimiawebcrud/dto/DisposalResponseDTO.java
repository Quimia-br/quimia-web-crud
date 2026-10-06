package com.quimia.quimiawebcrud.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DisposalResponseDTO extends BaseResponseDTO {

    private Integer productId;
    private String productDisposalMethod;
    private String packagingDisposalMethod;
    private String disposalRestrictions;
    private String environmentalPrecautions;
}
