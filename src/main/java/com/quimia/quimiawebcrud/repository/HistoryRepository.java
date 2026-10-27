package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.History;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<History, Integer> {

    @Override
    @EntityGraph(attributePaths = "products")
    List<History> findAll();
}
