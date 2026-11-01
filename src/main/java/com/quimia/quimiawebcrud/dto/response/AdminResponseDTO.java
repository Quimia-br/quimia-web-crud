package com.quimia.quimiawebcrud.dto.response;

import com.quimia.quimiawebcrud.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Dados de administrador que as telas mostram. Não leva a senha.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdminResponseDTO extends BaseResponseDTO {

    private String userName;
    private String email;
    private LocalDateTime signUpDate;
    private Status status;
}
