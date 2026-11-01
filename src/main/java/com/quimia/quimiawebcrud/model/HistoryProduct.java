package com.quimia.quimiawebcrud.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Liga uma consulta a um produto que o usuário tem (tabela {@code historicos_produtos}). A chave estrangeira é composta por usuário e produto, por isso o {@code @JoinColumns}.
 */
@Entity
@Table(name = "historicos_produtos")
@AttributeOverride(name = "id", column = @Column(name = "id_historico_produto"))
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class HistoryProduct extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_historico", nullable = false)
    private History history;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario", nullable = false),
            @JoinColumn(name = "id_produto", referencedColumnName = "id_produto", nullable = false)
    })
    private UserProduct userProduct;
}
