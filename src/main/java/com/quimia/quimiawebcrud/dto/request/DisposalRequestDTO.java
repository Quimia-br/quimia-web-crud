package com.quimia.quimiawebcrud.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dados que o formulário de descarte envia, já validados.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisposalRequestDTO {

    @NotNull(message = "Selecione o produto.")
    private Integer productId;
    private String productDisposalMethod;
    private String packagingDisposalMethod;
    private String disposalRestrictions;
    private String environmentalPrecautions;
}
