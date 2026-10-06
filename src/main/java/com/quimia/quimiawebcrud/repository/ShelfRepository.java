package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelfRepository extends JpaRepository<Shelf, Integer> {
}
