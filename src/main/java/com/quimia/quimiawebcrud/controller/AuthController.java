package com.quimia.quimiawebcrud.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela de login. Quem já está logado é levado direto para o painel.
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(Authentication authentication) {
        boolean loggedIn = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        return loggedIn ? "redirect:/" : "auth/login";
    }
}
