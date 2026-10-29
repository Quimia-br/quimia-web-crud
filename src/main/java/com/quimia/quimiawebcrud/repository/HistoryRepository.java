package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.History;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryRepository extends ColumnFilterRepository<History, Integer> {
}
