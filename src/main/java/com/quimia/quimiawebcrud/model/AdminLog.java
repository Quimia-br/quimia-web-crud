package com.quimia.quimiawebcrud.model;

import com.quimia.quimiawebcrud.model.enums.AdminAction;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Uma linha do log de edições: qual administrador fez o quê, em qual tabela e em qual registro.
 * Nas edições, {@code previousData} guarda o resumo do registro antes da mudança; nas exclusões, guarda também a
 * cópia das linhas apagadas, que é o que permite restaurar (ver {@code RemovalLogFormat}).
 */
@Entity
@Table(name = "admin_log_edicoes")
@AttributeOverride(name = "id", column = @Column(name = "id_admin_log_edicao"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class AdminLog extends Model {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_admin", nullable = false)
    @ToString.Exclude
    private Admin admin;

    @Column(name = "tabela_afetada", nullable = false, length = 150)
    private String affectedTable;

    @Column(name = "id_registro", nullable = false)
    private Integer recordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false, length = 50)
    private AdminAction action;

    @Column(name = "dado_anterior", columnDefinition = "TEXT")
    private String previousData;

    @Column(name = "data_edicao", nullable = false)
    private LocalDateTime editDate;

    @PrePersist
    private void prePersist() {
        if (editDate == null) editDate = LocalDateTime.now();
    }
}
