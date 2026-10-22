package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.exception.ResourceNotFoundException;
import com.quimia.quimiawebcrud.mapper.ResponseMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Consulta de uma tabela. As tabelas que só o aplicativo grava (estantes, históricos, localizações...) e o log
 * de edições ficam só com esta parte: a tela não tem como alterá-las.
 */
@Transactional(readOnly = true)
public abstract class ReadService<E, ID, Res> {

    protected abstract JpaRepository<E, ID> getRepository();

    protected abstract ResponseMapper<E, Res> getMapper();

    public List<Res> readAll() {
        List<Res> all = new ArrayList<>();
        for (E e : getRepository().findAll()) {
            all.add(getMapper().toResponse(e));
        }
        return all;
    }

    public Res findById(ID id) {
        return getMapper().toResponse(getEntity(id));
    }

    protected E getEntity(ID id) {
        return getRepository().findById(id)
                .orElseThrow(() -> notFound(id));
    }

    protected ResourceNotFoundException notFound(ID id) {
        return new ResourceNotFoundException("Registro não encontrado com o ID: " + id);
    }
}
