package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {
}
