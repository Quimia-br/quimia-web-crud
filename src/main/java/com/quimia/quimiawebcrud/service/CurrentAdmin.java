package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * O administrador da sessão atual (o nome do usuário na sessão é o e-mail dele).
 */
@Component
@RequiredArgsConstructor
public class CurrentAdmin {

    private final AdminRepository admins;

    public Admin require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Ação sem administrador autenticado.");
        }
        return admins.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Administrador da sessão não encontrado."));
    }
}
