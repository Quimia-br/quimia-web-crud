package com.quimia.quimiawebcrud.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consulta feita por um usuário (tabela {@code historicos}). A estante e o resultado são opcionais; os produtos consultados ficam em {@code historicos_produtos}.
 */
@Entity
@Table(name = "historicos")
@AttributeOverride(name = "id", column = @Column(name = "id_historico"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class History extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estante")
    private Shelf shelf;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_historico", nullable = false)
    private HistoryType historyType;

    @Column(name = "data_execucao", nullable = false)
    private LocalDateTime executionDate;

    @Column(name = "descricao_resultado", length = 255)
    private String resultDescription;

    @ManyToMany
    @JoinTable(
            name = "historicos_produtos",
            joinColumns = @JoinColumn(name = "id_historico"),
            inverseJoinColumns = @JoinColumn(name = "id_produto")
    )
    private List<Product> products;

    @PrePersist
    private void prePersist() {
        if (executionDate == null) executionDate = LocalDateTime.now();
    }
}
