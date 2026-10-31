package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.model.BaseEntity;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.model.Product;

/**
 * Atalhos dos mappers para ligar uma entidade a outra só pelo id: {@code company(5)} devolve uma {@code Company} com
 * apenas o id preenchido, que basta para o JPA gravar a chave estrangeira sem ir ao banco buscar a empresa inteira.
 */
final class References {

    private References() {
    }

    static Company company(Integer id) {
        return id == null ? null : Company.builder().id(id).build();
    }

    static Product product(Integer id) {
        return id == null ? null : Product.builder().id(id).build();
    }

    static Integer idOf(BaseEntity entity) {
        return entity == null ? null : entity.getId();
    }
}
