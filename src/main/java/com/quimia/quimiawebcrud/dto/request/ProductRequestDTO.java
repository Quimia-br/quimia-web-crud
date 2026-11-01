package com.quimia.quimiawebcrud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.quimia.quimiawebcrud.validation.FormPatterns;
import com.quimia.quimiawebcrud.validation.ValidCas;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dados que o formulário de produto envia, já validados.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDTO {

    @NotNull(message = "Selecione a empresa.")
    private Integer companyId;
    @NotBlank(message = "Informe o número CAS.")
    @Pattern(regexp = FormPatterns.CAS, message = "O número CAS deve ter o formato 0000-00-0.")
    @ValidCas
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
