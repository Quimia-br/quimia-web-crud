package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Disposal;
import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepository;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao banco para descartes; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface DisposalRepository extends ColumnFilterRepository<Disposal, Integer> {

    boolean existsByProductId(Integer productId);

    boolean existsByProductIdAndIdNot(Integer productId, Integer id);
}
