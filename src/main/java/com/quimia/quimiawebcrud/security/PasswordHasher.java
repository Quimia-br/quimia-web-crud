package com.quimia.quimiawebcrud.security;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * Único ponto do sistema que transforma senhas em hash e confere se uma senha bate com o hash salvo.
 */
@Component
public class PasswordHasher {

    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
