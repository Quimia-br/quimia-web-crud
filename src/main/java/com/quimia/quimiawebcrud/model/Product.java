package com.quimia.quimiawebcrud.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Produto químico cadastrado por uma empresa (tabela {@code produtos}), com o número CAS único.
 */
@Entity
@Table(name = "produtos")
@AttributeOverride(name = "id", column = @Column(name = "id_produto"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_empresa", nullable = false)
    private Company company;

    @Column(name = "cas_number", nullable = false, unique = true, length = 12)
    private String casNumber;

    @Column(name = "nome", nullable = false, length = 150)
    private String name;

    @Column(name = "marca", nullable = false, length = 150)
    private String brand;

    @Column(name = "instrucao_de_uso", nullable = false, columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "dosagem_tecnica", nullable = false, columnDefinition = "TEXT")
    private String dosage;

    @PrePersist
    private void prePersist() {
        if (brand == null || brand.isBlank()) brand = UNKNOWN;
    }
}
