package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Company;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends ColumnFilterRepository<Company, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Integer id);
}
