package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.model.Model;
import com.quimia.quimiawebcrud.model.Product;

final class References {

    private References() {
    }

    static Company company(Integer id) {
        return id == null ? null : Company.builder().id(id).build();
    }

    static Product product(Integer id) {
        return id == null ? null : Product.builder().id(id).build();
    }

    static Integer idOf(Model entity) {
        return entity == null ? null : entity.getId();
    }
}
