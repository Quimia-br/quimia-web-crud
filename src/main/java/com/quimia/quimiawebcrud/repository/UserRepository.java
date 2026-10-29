package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends ColumnFilterRepository<User, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
}
