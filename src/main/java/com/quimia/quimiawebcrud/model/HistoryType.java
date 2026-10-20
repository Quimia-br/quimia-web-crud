package com.quimia.quimiawebcrud.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tipos_historicos")
@AttributeOverride(name = "id", column = @Column(name = "id_tipo_historico"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class HistoryType extends Model {

    @Column(name = "nome", nullable = false, unique = true, length = 150)
    private String name;
}
