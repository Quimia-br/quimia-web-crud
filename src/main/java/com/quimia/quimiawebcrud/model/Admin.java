package com.quimia.quimiawebcrud.model;

import com.quimia.quimiawebcrud.model.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Administrador do painel: é quem entra no sistema (pelo e-mail e senha) e cadastra, edita e exclui os dados.
 * A senha é guardada só como hash ({@code senha_hash}).
 */
@Entity
@Table(name = "admins")
@AttributeOverride(name = "id", column = @Column(name = "id_admin"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class Admin extends Model {

    @Column(name = "nome", nullable = false, length = 150)
    private String userName;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime signUpDate;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status;

    // Valores padrão para o registro criado sem eles (data de cadastro de agora e conta ativa).
    @PrePersist
    private void prePersist() {
        if (signUpDate == null) signUpDate = LocalDateTime.now();
        if (status == null) status = Status.ATIVO;
    }
}
