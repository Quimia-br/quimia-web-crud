package com.quimia.quimiawebcrud.dto.response;

import com.quimia.quimiawebcrud.model.enums.CompanySize;
import com.quimia.quimiawebcrud.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Dados de empresa que as telas mostram. Não leva a senha.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CompanyResponseDTO extends BaseResponseDTO {

    private String cnpj;
    private String name;
    private String email;
    private CompanySize size;
    private LocalDateTime signUpDate;
    private Status status;
    private String photoUrl;
}
