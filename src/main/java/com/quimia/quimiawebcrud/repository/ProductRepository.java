package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Product;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends ColumnFilterRepository<Product, Integer> {

    boolean existsByCasNumber(String casNumber);

    boolean existsByCasNumberAndIdNot(String casNumber, Integer id);
}
