package com.quimia.quimiawebcrud.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base das entidades que têm uma chave numérica própria, gerada pelo banco. Não é uma tabela: o campo {@code id}
 * entra na tabela de cada entidade filha, que escolhe o nome da coluna com {@code @AttributeOverride}.
 * O {@code @SuperBuilder} faz o builder das filhas aceitar também o {@code id}.
 */
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class BaseEntity {

    /** Texto gravado quando um campo obrigatório chega vazio; é o mesmo valor que o banco usa. */
    protected static final String UNKNOWN = "DESCONHECIDO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
}
