package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Shelf;
import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepository;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao banco para estantes; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface ShelfRepository extends ColumnFilterRepository<Shelf, Integer> {
}
