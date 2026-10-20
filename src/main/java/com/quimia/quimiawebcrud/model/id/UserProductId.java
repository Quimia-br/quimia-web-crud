package com.quimia.quimiawebcrud.model.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

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
