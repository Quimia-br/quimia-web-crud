package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acesso ao banco para registros do log de edições; herda as consultas por coluna do {@link ColumnFilterRepository}.
 */
@Repository
public interface AdminLogRepository extends ColumnFilterRepository<AdminLog, Integer> {

    /**
     * Uma linha por registro do log, sem trazer o texto grande: id, tabela, id do registro, ação e se há cópia
     * para restaurar (o marcador de RemovalLogFormat).
     */
    @Query("select l.id, l.affectedTable, l.recordId, l.action, "
            + "case when l.previousData like '%---dados-para-restaurar---%' then true else false end "
            + "from AdminLog l")
    List<Object[]> findRestoreInfo();
}
