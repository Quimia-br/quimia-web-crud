package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.model.*;
import com.quimia.quimiawebcrud.model.id.UserProductId;

final class References {

    private References() {
    }

    static Admin admin(Integer id) {
        return id == null ? null : Admin.builder().id(id).build();
    }

    static Company company(Integer id) {
        return id == null ? null : Company.builder().id(id).build();
    }

    static User user(Integer id) {
        return id == null ? null : User.builder().id(id).build();
    }

    static Product product(Integer id) {
        return id == null ? null : Product.builder().id(id).build();
    }

    static Shelf shelf(Integer id) {
        return id == null ? null : Shelf.builder().id(id).build();
    }

    static HistoryType historyType(Integer id) {
        return id == null ? null : HistoryType.builder().id(id).build();
    }

    static History history(Integer id) {
        return id == null ? null : History.builder().id(id).build();
    }

    static UserProduct userProduct(Integer userId, Integer productId) {
        if (userId == null || productId == null) {
            return null;
        }
        return UserProduct.builder().id(new UserProductId(userId, productId)).build();
    }

    static Integer idOf(Model entity) {
        return entity == null ? null : entity.getId();
    }
}
