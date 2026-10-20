package com.quimia.quimiawebcrud.model;

import com.quimia.quimiawebcrud.model.id.UserProductId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "produtos_usuarios")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
public class UserProduct {

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
