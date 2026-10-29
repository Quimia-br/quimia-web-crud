package com.quimia.quimiawebcrud.model.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

/**
 * Chave composta de {@code UserProduct}: usuário + produto (um produto aparece uma vez na lista de cada usuário).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserProductId implements Serializable {

    @Column(name = "id_usuario", nullable = false)
    private Integer userId;

    @Column(name = "id_produto", nullable = false)
    private Integer productId;
}
