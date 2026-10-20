package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.CompanySize;
import com.quimia.quimiawebcrud.model.enums.Status;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRequestDTO {

    private String cnpj;
    private String name;
    private String email;
    private String password;
    private CompanySize size;
    private Status status;
    private String photoUrl;
    private MultipartFile photo;
}
