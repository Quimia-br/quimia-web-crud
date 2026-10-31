package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Product;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao banco para produtos; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface ProductRepository extends ColumnFilterRepository<Product, Integer> {

    boolean existsByCasNumber(String casNumber);

    boolean existsByCasNumberAndIdNot(String casNumber, Integer id);
}
