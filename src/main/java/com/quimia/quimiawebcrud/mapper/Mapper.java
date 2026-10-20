package com.quimia.quimiawebcrud.mapper;

public interface Mapper<E, Req, Res> {

    E toEntity(Req request);

    Res toResponse(E entity);

    void updateEntity(E entity, Req request);
}
