package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.exception.ResourceNotFoundException;
import com.quimia.quimiawebcrud.mapper.support.ResponseMapper;
import com.quimia.quimiawebcrud.repository.support.ColumnFilterRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Consulta de uma tabela. As tabelas que só o aplicativo grava (estantes, históricos, localizações...) e o log
 * de edições ficam só com esta parte: a tela não tem como alterá-las.
 */
@Transactional(readOnly = true)
public abstract class ReadService<E, ID, Res> {

    /** O repositório da tabela. */
    protected abstract ColumnFilterRepository<E, ID> getRepository();

    /** O mapper que converte a entidade no que a tela mostra. */
    protected abstract ResponseMapper<E, Res> getMapper();

    /** Todos os registros da tabela, já convertidos para a tela. */
    public List<Res> readAll() {
        List<Res> all = new ArrayList<>();
        for (E e : getRepository().findAll()) {
            all.add(getMapper().toResponse(e));
        }
        return all;
    }

    /** Um registro pelo id; ResourceNotFoundException se não existir. */
    public Res findById(ID id) {
        return getMapper().toResponse(getEntity(id));
    }

    /** A entidade pelo id, para quem precisa alterá-la; ResourceNotFoundException se não existir. */
    protected E getEntity(ID id) {
        return getRepository().findById(id)
                .orElseThrow(() -> notFound(id));
    }

    /**
     * A exceção de registro inexistente; as tabelas de chave composta sobrescrevem para citar as duas partes.
     */
    protected ResourceNotFoundException notFound(ID id) {
        return new ResourceNotFoundException("Registro não encontrado com o ID: " + id);
    }
}
