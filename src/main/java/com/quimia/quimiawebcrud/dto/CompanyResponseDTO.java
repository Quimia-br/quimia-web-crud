package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.CompanySize;
import com.quimia.quimiawebcrud.model.enums.Status;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

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
