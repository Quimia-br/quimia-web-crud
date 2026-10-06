package com.quimia.quimiawebcrud.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "estantes")
@AttributeOverride(name = "id", column = @Column(name = "id_estante"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class Shelf extends Model {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "comodo", nullable = false, length = 150)
    private String room;

    @PrePersist
    private void prePersist() {
        if (room == null || room.isBlank()) room = "DESCONHECIDO";
    }
}
