package com.quimia.quimiawebcrud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do painel administrativo da Quimia. Fica no pacote raiz para que o Spring encontre
 * todas as classes dos pacotes de dentro (controller, service, repository...).
 */
@SpringBootApplication
public class QuimiaWebCrudApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuimiaWebCrudApplication.class, args);
    }
}