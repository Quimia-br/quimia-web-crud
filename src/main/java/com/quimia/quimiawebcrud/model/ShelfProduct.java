package com.quimia.quimiawebcrud.model;

import com.quimia.quimiawebcrud.model.id.ShelfProductId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Produto guardado em uma estante, com a data em que foi colocado. A chave é a dupla estante + produto.
 */
@Entity
@Table(name = "produtos_estantes")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class ShelfProduct {

    // As colunas de estante e produto são gravadas pela chave; as relações abaixo só servem para leitura
    // (insertable/updatable = false), para o Hibernate não gravar a mesma coluna duas vezes.
    @EmbeddedId
    private ShelfProductId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estante", insertable = false, updatable = false)
    @ToString.Exclude
    private Shelf shelf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_produto", insertable = false, updatable = false)
    @ToString.Exclude
    private Product product;

    @Column(name = "data_adicao", nullable = false)
    private LocalDateTime addedAt;

    @PrePersist
    private void prePersist() {
        if (addedAt == null) addedAt = LocalDateTime.now();
    }
}
