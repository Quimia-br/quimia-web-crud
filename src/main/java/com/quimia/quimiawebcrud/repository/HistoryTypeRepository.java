package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.HistoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryTypeRepository extends JpaRepository<HistoryType, Integer> {
}
