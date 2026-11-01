package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.HistoryType;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao banco para tipos de histórico; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface HistoryTypeRepository extends ColumnFilterRepository<HistoryType, Integer> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
