package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.mapper.Mapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Transactional(readOnly = true)
public abstract class CrudService<E, ID, Req, Res> {

    protected abstract JpaRepository<E, ID> getRepository();

    protected abstract Mapper<E, Req, Res> getMapper();

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

    @Transactional
    public Res create(Req request) {
        E entity = getRepository().save(getMapper().toEntity(request));
        return getMapper().toResponse(entity);
    }

    @Transactional
    public Res update(ID id, Req request) {
        E entity = getEntity(id);
        getMapper().updateEntity(entity, request);
        return getMapper().toResponse(entity);
    }

    @Transactional
    public void delete(ID id) {
        getRepository().delete(getEntity(id));
    }

    protected E getEntity(ID id) {
        return getRepository().findById(id)
                .orElseThrow(() -> notFound(id));
    }

    protected RuntimeException notFound(ID id) {
        return new RuntimeException("Registro não encontrado com o ID: " + id);
    }
}
