package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.ShelfProduct;
import com.quimia.quimiawebcrud.model.id.ShelfProductId;
import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepository;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao banco para produtos de estantes; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface ShelfProductRepository extends ColumnFilterRepository<ShelfProduct, ShelfProductId> {
}
