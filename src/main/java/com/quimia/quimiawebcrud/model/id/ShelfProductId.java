package com.quimia.quimiawebcrud.model.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Chave composta de {@code produtos_estantes}: um registro por par (estante, produto). Só o aplicativo grava essa
 * tabela; o painel apenas lê. As duas colunas juntas identificam a linha, por isso {@code equals} e
 * {@code hashCode} usam as duas, e o JPA exige que a classe seja {@code Serializable}.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ShelfProductId implements Serializable {

    @Column(name = "id_estante", nullable = false)
    private Integer shelfId;

    @Column(name = "id_produto", nullable = false)
    private Integer productId;
}
