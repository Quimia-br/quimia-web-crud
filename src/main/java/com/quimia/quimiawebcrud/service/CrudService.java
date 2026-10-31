package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.mapper.Mapper;
import com.quimia.quimiawebcrud.model.BaseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Cadastro de uma tabela que a tela cria, edita e exclui. Toda criação e edição é conferida
 * ({@link #validate}) e registrada no log de edições; a exclusão fica a cargo de cada tabela,
 * que apaga junto o que depende do registro (ver {@link RecordRemovalService}).
 */
public abstract class CrudService<E extends BaseEntity, ID, Req, Res> extends ReadService<E, ID, Res> {

    @Override
    protected abstract Mapper<E, Req, Res> getMapper();

    /** O registrador do log de edições usado em toda criação e edição. */
    protected abstract EditLog getEditLog();

    /** Valida, grava o registro novo e o anota no log de edições. */
    @Transactional
    public Res create(Req request) {
        validate(request, null);
        E entity = getRepository().save(newEntity(request));
        getEditLog().created(entity);
        return getMapper().toResponse(entity);
    }

    /** Valida, aplica as mudanças e anota no log o resumo do registro antes da edição. */
    @Transactional
    public Res update(ID id, Req request) {
        E entity = getEntity(id);
        validate(request, id);
        String before = RecordSummary.of(entity);
        applyChanges(entity, request);
        getEditLog().updated(entity, before);
        return getMapper().toResponse(entity);
    }

    /** Exclui o registro. Cada tabela apaga junto o que depende dele e guarda a cópia no log. */
    public abstract void delete(ID id);

    /**
     * Altera, em todos os registros em que {@code column} é igual a {@code value}, os campos de {@code changes}
     * (coluna e novo valor). Não passa pelo {@link #validate}: só as colunas que a tela oferece para isso,
     * que não têm regra de cadastro além do que o banco confere. Cada registro alterado entra no log de edições.
     * Devolve quantos registros foram alterados.
     */
    @Transactional
    public int updateByColumn(String column, String value, Map<String, String> changes) {
        List<E> targets = getRepository().findByColumn(column, value);
        if (targets.isEmpty()) {
            return 0;
        }
        List<String> before = targets.stream().map(RecordSummary::of).toList();
        int updated = getRepository().updateByColumn(column, value, changes);
        for (int i = 0; i < targets.size(); i++) {
            getEditLog().updated(targets.get(i), before.get(i));
        }
        return updated;
    }

    /**
     * Exclui todos os registros em que {@code column} é igual a {@code value}, cada um pelo {@link #delete} da tela:
     * apaga junto o que depende dele e guarda a cópia no log de edições. Se algum não puder ser excluído, nenhum é.
     * Devolve quantos registros foram excluídos.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public int deleteByColumn(String column, String value) {
        List<E> targets = getRepository().findByColumn(column, value);
        for (E target : targets) {
            delete((ID) target.getId());
        }
        return targets.size();
    }

    /**
     * Regras que o pedido precisa cumprir antes de gravar; {@code id} é nulo na criação.
     */
    protected void validate(Req request, ID id) {
    }

    /** Monta a entidade nova a partir do formulário; as tabelas com senha ou foto sobrescrevem. */
    protected E newEntity(Req request) {
        return getMapper().toEntity(request);
    }

    /** Aplica o formulário numa entidade existente; as tabelas com senha ou foto sobrescrevem. */
    protected void applyChanges(E entity, Req request) {
        getMapper().updateEntity(entity, request);
    }
}
