package com.quimia.quimiawebcrud.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Base das entidades com chave numérica gerada pelo banco (IDENTITY). Cada tabela tem um nome próprio para a coluna
 * do id, definido na entidade com {@code @AttributeOverride} (ex.: {@code id_usuario}, {@code id_produto}).
 */
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
}
