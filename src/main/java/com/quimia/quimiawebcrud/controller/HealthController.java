package com.quimia.quimiawebcrud.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responde "ok" se o app está de pé. É o endereço que o Render consulta para saber se o serviço está vivo.
 * Não toca no banco de propósito: se o banco estiver desligado, o app não deve ser reiniciado em loop por isso.
 */
@RestController
public class HealthController {

    /** Devolve "ok" sem exigir login e sem consultar nada. */
    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
