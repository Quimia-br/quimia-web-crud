package com.quimia.quimiawebcrud.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void producesArgon2idHashesInTheStandardFormat() {
        String hash = hasher.hash("Segredo#1");

        assertTrue(hash.startsWith("$argon2id$v=19$"), hash);
        assertTrue(hash.length() <= 255, "fits the senha_hash VARCHAR(255) column");
        assertEquals(hash.length(), 97, "same size as the hashes already stored in the database");
    }

    @Test
    void everyHashHasItsOwnRandomSalt() {
        assertNotEquals(hasher.hash("Segredo#1"), hasher.hash("Segredo#1"));
    }

    @Test
    void confirmsOnlyTheRightPassword() {
        String hash = hasher.hash("Segredo#1");

        assertTrue(hasher.matches("Segredo#1", hash));
        assertFalse(hasher.matches("segredo#1", hash));
        assertFalse(hasher.matches("", hash));
    }

    @Test
    void acceptsHashesCreatedWithOtherArgon2Parameters() {
        // a base populada por outra ferramenta usa m=65536,t=3,p=4; os parametros vem dentro do proprio hash
        String external = new Argon2PasswordEncoder(16, 32, 4, 65536, 3).encode("Outra#Senha");

        assertTrue(external.contains("m=65536,t=3,p=4"));
        assertTrue(hasher.matches("Outra#Senha", external));
        assertFalse(hasher.matches("errada", external));
    }

    @Test
    void doesNotAcceptAnythingThatIsNotAnArgon2Hash() {
        assertFalse(hasher.matches("Segredo#1", "Segredo#1"));
        assertFalse(hasher.matches("Segredo#1", "$2a$10$abcdefghijklmnopqrstuuMnqIwLQnQh4Z2fA8h8g9b5y0yWm0e1a"));
    }
}
