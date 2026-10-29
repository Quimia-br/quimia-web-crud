package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.Location;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends ColumnFilterRepository<Location, Integer> {
}
