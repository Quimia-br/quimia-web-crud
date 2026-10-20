package com.quimia.quimiawebcrud.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "produtos")
@AttributeOverride(name = "id", column = @Column(name = "id_produto"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class Product extends Model {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_empresa", nullable = false)
    @ToString.Exclude
    private Company company;

    @Column(name = "cas_number", nullable = false, unique = true, length = 12)
    private String casNumber;

    @Column(name = "nome", nullable = false, length = 150)
    private String name;

    @Column(name = "marca", nullable = false, length = 150)
    private String brand;

    @Column(name = "instrucoes_de_uso", nullable = false, columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "dosagem_tecnica", nullable = false, columnDefinition = "TEXT")
    private String dosage;

    @PrePersist
    private void prePersist() {
        if (brand == null || brand.isBlank()) brand = "DESCONHECIDO";
    }
}
