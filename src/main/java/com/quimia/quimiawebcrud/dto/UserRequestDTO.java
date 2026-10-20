package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.Status;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDTO {

    private String userName;
    private LocalDate birthDate;
    private String email;
    private String password;
    private Status status;
    private String photoUrl;
}
