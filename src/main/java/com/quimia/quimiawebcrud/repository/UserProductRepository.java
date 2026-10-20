package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.UserProduct;
import com.quimia.quimiawebcrud.model.id.UserProductId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProductRepository extends JpaRepository<UserProduct, UserProductId> {
}
