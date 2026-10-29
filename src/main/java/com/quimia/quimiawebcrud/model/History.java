package com.quimia.quimiawebcrud.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de uso feito por um usuário no aplicativo (uma consulta de um certo tipo, com data e resultado).
 * A estante é opcional. O painel só lê esta tabela: quem grava é o aplicativo.
 */
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

    // Só para leitura (nomes dos produtos no relatório). A tabela historicos_produtos tem id próprio e exige
    // id_usuario, então gravar por este mapeamento falharia; a gravação é a da entidade HistoryProduct.
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
