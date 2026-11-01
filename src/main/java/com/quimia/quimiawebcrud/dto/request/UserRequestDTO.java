package com.quimia.quimiawebcrud.dto.request;

import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.validation.FormPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * Dados que o formulário de usuário envia, já validados.
 */
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
    @Email(regexp = FormPatterns.EMAIL, message = "Informe um e-mail completo, como nome@dominio.com.")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    private String email;
    @Pattern(regexp = FormPatterns.PASSWORD, message = "A senha deve ter de 8 a 72 caracteres.")
    private String password;
    @NotNull(message = "Selecione o status.")
    private Status status;
    private String photoUrl;
    private MultipartFile photo;
}
