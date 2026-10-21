package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Past;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDTO {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    private String userName;
    @Past(message = "A data de nascimento deve estar no passado.")
    private LocalDate birthDate;
    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    private String email;
    @Pattern(regexp = "^$|.{8,72}", message = "A senha deve ter de 8 a 72 caracteres.")
    private String password;
    @NotNull(message = "Selecione o status.")
    private Status status;
    private String photoUrl;
    private MultipartFile photo;
}
