package com.quimia.quimiawebcrud.model;

import com.quimia.quimiawebcrud.model.id.ShelfProductId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "produtos_estantes")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class ShelfProduct {

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
