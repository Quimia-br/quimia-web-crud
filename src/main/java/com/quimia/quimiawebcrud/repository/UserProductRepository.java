package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.UserProduct;
import com.quimia.quimiawebcrud.model.id.UserProductId;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao banco para produtos de usuários; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface UserProductRepository extends ColumnFilterRepository<UserProduct, UserProductId> {
}
