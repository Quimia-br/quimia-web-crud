package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.mapper.Mapper;
import com.quimia.quimiawebcrud.model.Model;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de uma tabela que a tela cria, edita e exclui. Toda criação e edição é conferida
 * ({@link #validate}) e registrada no log de edições; a exclusão fica a cargo de cada tabela,
 * que apaga junto o que depende do registro (ver {@link RecordRemovalService}).
 */
public abstract class CrudService<E extends Model, ID, Req, Res> extends ReadService<E, ID, Res> {

    @Override
    protected abstract Mapper<E, Req, Res> getMapper();

    protected abstract EditLog getEditLog();

    @Transactional
    public Res create(Req request) {
        validate(request, null);
        E entity = getRepository().save(newEntity(request));
        getEditLog().created(entity);
        return getMapper().toResponse(entity);
    }

    @Transactional
    public Res update(ID id, Req request) {
        E entity = getEntity(id);
        validate(request, id);
        String before = RecordSummary.of(entity);
        applyChanges(entity, request);
        getEditLog().updated(entity, before);
        return getMapper().toResponse(entity);
    }

    public abstract void delete(ID id);

    /**
     * Regras que o pedido precisa cumprir antes de gravar; {@code id} é nulo na criação.
     */
    protected void validate(Req request, ID id) {
    }

    protected E newEntity(Req request) {
        return getMapper().toEntity(request);
    }

    protected void applyChanges(E entity, Req request) {
        getMapper().updateEntity(entity, request);
    }
}
