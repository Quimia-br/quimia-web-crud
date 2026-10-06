package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.ShelfProduct;
import com.quimia.quimiawebcrud.model.id.ShelfProductId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelfProductRepository extends JpaRepository<ShelfProduct, ShelfProductId> {
}
