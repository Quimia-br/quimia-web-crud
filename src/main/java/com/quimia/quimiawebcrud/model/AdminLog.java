package com.quimia.quimiawebcrud.model;

import com.quimia.quimiawebcrud.model.enums.AdminAction;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Registro do que um administrador fez: tabela afetada, id do registro, ação e o dado anterior (tabela {@code admin_log_edicoes}). É o que permite desfazer uma remoção.
 */
@Entity
@Table(name = "admin_log_edicoes")
@AttributeOverride(name = "id", column = @Column(name = "id_admin_log_edicao"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class AdminLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_admin", nullable = false)
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
