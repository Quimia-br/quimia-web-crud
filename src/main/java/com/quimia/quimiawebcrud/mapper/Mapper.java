package com.quimia.quimiawebcrud.mapper;

/**
 * Mapper das tabelas que a tela cadastra e edita: além de mostrar, monta e altera a entidade.
 */
public interface Mapper<E, Req, Res> extends ResponseMapper<E, Res> {

    E toEntity(Req request);

    void updateEntity(E entity, Req request);
}
