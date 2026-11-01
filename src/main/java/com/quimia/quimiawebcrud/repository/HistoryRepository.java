package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.History;
import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acesso ao banco para históricos; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface HistoryRepository extends ColumnFilterRepository<History, Integer> {

    @Override
    @EntityGraph(attributePaths = "products")
    List<History> findAll();
}
