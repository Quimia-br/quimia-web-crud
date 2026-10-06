package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.Status;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

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
