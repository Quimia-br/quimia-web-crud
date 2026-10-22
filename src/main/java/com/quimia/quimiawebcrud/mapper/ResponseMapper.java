package com.quimia.quimiawebcrud.mapper;

/**
 * Converte a entidade no que a tela mostra. Tabelas que a tela só consulta têm apenas este mapper.
 */
public interface ResponseMapper<E, Res> {

    Res toResponse(E entity);
}
