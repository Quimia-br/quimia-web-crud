package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryRepository extends JpaRepository<History, Integer> {
}
