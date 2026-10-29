package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Disposal;
import org.springframework.stereotype.Repository;

@Repository
public interface DisposalRepository extends ColumnFilterRepository<Disposal, Integer> {

    boolean existsByProductId(Integer productId);

    boolean existsByProductIdAndIdNot(Integer productId, Integer id);
}
