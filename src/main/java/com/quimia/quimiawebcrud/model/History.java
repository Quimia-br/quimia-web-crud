package com.quimia.quimiawebcrud.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "historicos")
@AttributeOverride(name = "id", column = @Column(name = "id_historico"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class History extends Model {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estante")
    @ToString.Exclude
    private Shelf shelf;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_historico", nullable = false)
    @ToString.Exclude
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
    @ToString.Exclude
    private List<Product> products;

    @PrePersist
    private void prePersist() {
        if (executionDate == null) executionDate = LocalDateTime.now();
    }
}
