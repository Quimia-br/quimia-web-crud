package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepository;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao banco para empresas; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface CompanyRepository extends ColumnFilterRepository<Company, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Integer id);
}
