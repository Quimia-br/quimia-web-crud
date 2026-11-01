package com.quimia.quimiawebcrud.mapper.support;

/**
 * Converte a entidade no que a tela mostra. Tabelas que a tela só consulta têm apenas este mapper.
 */
public interface ResponseMapper<E, Res> {

    Res toResponse(E entity);
}
