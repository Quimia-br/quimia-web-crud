package com.quimia.quimiawebcrud.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Estante de um usuário, identificada pelo cômodo onde fica (tabela {@code estantes}).
 */
@Entity
@Table(name = "estantes")
@AttributeOverride(name = "id", column = @Column(name = "id_estante"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class Shelf extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User user;

    @Column(name = "comodo", nullable = false, length = 150)
    private String room;

    @PrePersist
    private void prePersist() {
        if (room == null || room.isBlank()) room = UNKNOWN;
    }
}
