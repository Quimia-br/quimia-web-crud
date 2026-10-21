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
public class ProductRequestDTO {

    @NotNull(message = "Selecione a empresa.")
    private Integer companyId;
    @NotBlank(message = "Informe o número CAS.")
    @Pattern(regexp = "\\d{2,7}-\\d{2}-\\d", message = "O número CAS deve ter o formato 0000-00-0.")
    private String casNumber;
    @NotBlank(message = "Informe o nome.")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    private String name;
    @Size(max = 150, message = "A marca deve ter no máximo 150 caracteres.")
    private String brand;
    @NotBlank(message = "Informe as instruções de uso.")
    private String instructions;
    @NotBlank(message = "Informe a dosagem técnica.")
    private String dosage;
}
