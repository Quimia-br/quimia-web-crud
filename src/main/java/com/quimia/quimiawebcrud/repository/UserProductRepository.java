package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.UserProduct;
import com.quimia.quimiawebcrud.model.id.UserProductId;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProductRepository extends ColumnFilterRepository<UserProduct, UserProductId> {
}
