package com.quimia.quimiawebcrud.model;

import com.quimia.quimiawebcrud.model.id.ShelfProductId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Produto guardado numa estante (tabela {@code produtos_estantes}). A chave é composta; as colunas do id também alimentam as associações, por isso elas são só de leitura ({@code insertable = false, updatable = false}).
 */
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
    private Shelf shelf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_produto", insertable = false, updatable = false)
    private Product product;

    @Column(name = "data_adicao", nullable = false)
    private LocalDateTime addedAt;

    @PrePersist
    private void prePersist() {
        if (addedAt == null) addedAt = LocalDateTime.now();
    }
}
