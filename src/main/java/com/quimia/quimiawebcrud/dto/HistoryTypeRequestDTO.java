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
public class HistoryTypeRequestDTO {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    private String name;
}
