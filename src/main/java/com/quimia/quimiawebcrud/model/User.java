package com.quimia.quimiawebcrud.model;

import com.quimia.quimiawebcrud.model.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@AttributeOverride(name = "id", column = @Column(name = "id_usuario"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class User extends Model {

    @Column(name = "nome", nullable = false, length = 150)
    private String userName;

    @Column(name = "data_nascimento")
    private LocalDate birthDate;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime signUpDate;

    @Column(name = "senha_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status;

    @Column(name = "url_foto", columnDefinition = "TEXT")
    private String photoUrl;

    @PrePersist
    private void prePersist() {
        if (signUpDate == null) signUpDate = LocalDateTime.now();
        if (status == null) status = Status.ATIVO;
    }
}
