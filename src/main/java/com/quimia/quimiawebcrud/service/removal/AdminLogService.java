package com.quimia.quimiawebcrud.service.removal;

import com.quimia.quimiawebcrud.dto.response.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.mapper.AdminLogMapper;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import com.quimia.quimiawebcrud.service.ReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regras de registros do log de edições: somente leitura.
 */
@Service
@RequiredArgsConstructor
public class AdminLogService extends ReadService<AdminLog, Integer, AdminLogResponseDTO> {

    private final AdminLogRepository repository;
    private final AdminLogMapper mapper;

    @Override
    protected AdminLogRepository getRepository() {
        return repository;
    }

    @Override
    protected AdminLogMapper getMapper() {
        return mapper;
    }

    /** Todo o log, do registro mais novo para o mais antigo. */
    public List<AdminLogResponseDTO> readAllNewestFirst() {
        List<AdminLogResponseDTO> logs = readAll();
        logs.sort(Comparator.comparing(AdminLogResponseDTO::getEditDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return logs;
    }

    /**
     * Exclusões que ainda podem ser desfeitas: a última ação de cada registro é uma exclusão que guardou a cópia.
     * (Depois de restaurar, a restauração vira a última ação e a seta some; se for excluído de novo, a nova
     * exclusão é que passa a valer.)
     */
    public Set<Integer> restorableIds() {
        List<Object[]> rows = repository.findRestoreInfo();
        Map<String, Integer> latest = new HashMap<>();
        for (Object[] row : rows) {
            latest.merge(key(row), (Integer) row[0], Math::max);
        }
        Set<Integer> ids = new HashSet<>();
        for (Object[] row : rows) {
            Integer id = (Integer) row[0];
            if (row[3] == AdminAction.DELETE && Boolean.TRUE.equals(row[4]) && id.equals(latest.get(key(row)))) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static String key(Object[] row) {
        return row[1] + "#" + row[2];
    }
}
