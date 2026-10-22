package com.quimia.quimiawebcrud;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O banco compartilhado tem poucas conexões: o pool do app precisa ser pequeno e não manter todas abertas.
 */
class ConnectionPoolConfigTest {

    @Test
    void poolIsSmallAndDoesNotKeepEveryConnectionOpen() throws Exception {
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            properties.load(in);
        }
        int max = Integer.parseInt(properties.getProperty("spring.datasource.hikari.maximum-pool-size"));
        int minIdle = Integer.parseInt(properties.getProperty("spring.datasource.hikari.minimum-idle"));

        assertTrue(max >= 2 && max <= 8, "pool máximo: " + max);
        assertTrue(minIdle >= 1 && minIdle < max, "conexões mínimas paradas: " + minIdle);
    }
}
