package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.History;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryRepository extends ColumnFilterRepository<History, Integer> {

    @Override
    @EntityGraph(attributePaths = "products")
    List<History> findAll();
}
