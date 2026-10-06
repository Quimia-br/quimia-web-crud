package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.HistoryProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryProductRepository extends JpaRepository<HistoryProduct, Integer> {
}
