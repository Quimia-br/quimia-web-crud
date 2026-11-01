package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.HistoryProduct;
import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepository;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao banco para produtos de históricos; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface HistoryProductRepository extends ColumnFilterRepository<HistoryProduct, Integer> {
}
