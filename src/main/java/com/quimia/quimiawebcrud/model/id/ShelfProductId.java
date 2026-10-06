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
public class ShelfProductId implements Serializable {

    @Column(name = "id_estante", nullable = false)
    private Integer shelfId;

    @Column(name = "id_produto", nullable = false)
    private Integer productId;
}
