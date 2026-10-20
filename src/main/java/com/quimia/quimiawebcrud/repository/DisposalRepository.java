package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Disposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisposalRepository extends JpaRepository<Disposal, Integer> {
}
