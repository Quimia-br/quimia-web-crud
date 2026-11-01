package com.quimia.quimiawebcrud.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Dados de descarte que as telas mostram.
 */
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
