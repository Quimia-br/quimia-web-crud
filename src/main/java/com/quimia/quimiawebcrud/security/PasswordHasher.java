package com.quimia.quimiawebcrud.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Único ponto do sistema que transforma senhas em hash (Argon2id) e confere se uma senha bate com o hash salvo.
 * O hash gerado já carrega o sal e os parâmetros, no formato padrão "$argon2id$v=19$m=...,t=...,p=...$sal$hash".
 * Também é o PasswordEncoder usado pelo Spring Security no login.
 */
@Component
public class PasswordHasher implements PasswordEncoder {

    private final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public String hash(String rawPassword) {
        return encode(rawPassword);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
