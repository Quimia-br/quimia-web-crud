package com.quimia.quimiawebcrud.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Tipo de consulta que o usuário pode fazer no aplicativo, como descarte ou mistura (tabela {@code tipos_historicos}).
 */
@Entity
@Table(name = "tipos_historicos")
@AttributeOverride(name = "id", column = @Column(name = "id_tipo_historico"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class HistoryType extends BaseEntity {

    @Column(name = "nome", nullable = false, unique = true, length = 150)
    private String name;
}
