package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Shelf;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelfRepository extends ColumnFilterRepository<Shelf, Integer> {
}
