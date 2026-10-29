package com.quimia.quimiawebcrud.model;

import com.quimia.quimiawebcrud.model.id.UserProductId;
import jakarta.persistence.*;
import lombok.*;

/**
 * Produto na lista de um usuário. A chave é a dupla usuário + produto; é a esta lista que os itens do histórico se
 * ligam ({@code HistoryProduct}).
 */
@Entity
@Table(name = "produtos_usuarios")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class UserProduct {

    // As colunas de usuário e produto são gravadas pela chave; as relações abaixo só servem para leitura
    // (insertable/updatable = false), para o Hibernate não gravar a mesma coluna duas vezes.
    @EmbeddedId
    private UserProductId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_produto", insertable = false, updatable = false)
    @ToString.Exclude
    private Product product;
}
