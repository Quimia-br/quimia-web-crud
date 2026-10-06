package com.quimia.quimiawebcrud.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "descartes_fds")
@AttributeOverride(name = "id", column = @Column(name = "id_descarte_fds"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class Disposal extends Model {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_produto", nullable = false, unique = true)
    @ToString.Exclude
    private Product product;

    @Column(name = "metodo_descarte_produto", columnDefinition = "TEXT")
    private String productDisposalMethod;

    @Column(name = "metodo_descarte_embalagem", columnDefinition = "TEXT")
    private String packagingDisposalMethod;

    @Column(name = "restricao_descarte", columnDefinition = "TEXT")
    private String disposalRestrictions;

    @Column(name = "precaucao_ambiental", columnDefinition = "TEXT")
    private String environmentalPrecautions;
}
