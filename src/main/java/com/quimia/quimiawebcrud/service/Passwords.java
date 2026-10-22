package com.quimia.quimiawebcrud.service;

/**
 * Senha nos formulários de cadastro: obrigatória ao criar; ao editar, em branco mantém a atual.
 */
final class Passwords {

    private Passwords() {
    }

    static boolean present(String password) {
        return password != null && !password.isBlank();
    }
}
