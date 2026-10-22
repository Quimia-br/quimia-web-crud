package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByCasNumber(String casNumber);

    boolean existsByCasNumberAndIdNot(String casNumber, Integer id);
}
