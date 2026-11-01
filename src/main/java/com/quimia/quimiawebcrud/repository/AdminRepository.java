package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Admin;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acesso ao banco para administradores; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface AdminRepository extends ColumnFilterRepository<Admin, Integer> {

    Optional<Admin> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
}
