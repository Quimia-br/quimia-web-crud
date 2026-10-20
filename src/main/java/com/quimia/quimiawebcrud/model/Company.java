package com.quimia.quimiawebcrud.model;

import com.quimia.quimiawebcrud.model.enums.CompanySize;
import com.quimia.quimiawebcrud.model.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios_empresas")
@AttributeOverride(name = "id", column = @Column(name = "id_usuario_empresa"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class Company extends Model {

    @Column(name = "cnpj", nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(name = "nome", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "porte", nullable = false, length = 15)
    private CompanySize size;

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
        if (size == null) size = CompanySize.DESCONHECIDO;
        if (status == null) status = Status.ATIVO;
    }
}
