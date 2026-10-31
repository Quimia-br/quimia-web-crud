package com.quimia.quimiawebcrud.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Orientações de descarte da FDS (ficha de dados de segurança) de um produto (tabela {@code descartes_fds}); cada produto tem no máximo uma.
 */
@Entity
@Table(name = "descartes_fds")
@AttributeOverride(name = "id", column = @Column(name = "id_descarte_fds"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class Disposal extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_produto", nullable = false, unique = true)
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
