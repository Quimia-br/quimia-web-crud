package com.quimia.quimiawebcrud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

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
