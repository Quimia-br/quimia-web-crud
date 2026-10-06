package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Integer> {
}
