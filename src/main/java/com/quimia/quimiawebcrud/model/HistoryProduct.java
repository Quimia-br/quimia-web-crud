package com.quimia.quimiawebcrud.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "historicos_produtos")
@AttributeOverride(name = "id", column = @Column(name = "id_historico_produto"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class HistoryProduct extends Model {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_historico", nullable = false)
    @ToString.Exclude
    private History history;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario", nullable = false),
            @JoinColumn(name = "id_produto", referencedColumnName = "id_produto", nullable = false)
    })
    @ToString.Exclude
    private UserProduct userProduct;
}
