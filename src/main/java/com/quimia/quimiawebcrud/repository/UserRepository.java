package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepository;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao banco para usuários; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface UserRepository extends ColumnFilterRepository<User, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
}
