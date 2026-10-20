package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.Status;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRequestDTO {

    private String userName;
    private String email;
    private String password;
    private Status status;
}
