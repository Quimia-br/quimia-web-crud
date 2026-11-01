package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Location;
import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepository;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao banco para localizações; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface LocationRepository extends ColumnFilterRepository<Location, Integer> {
}
