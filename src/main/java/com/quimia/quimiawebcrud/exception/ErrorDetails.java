package com.quimia.quimiawebcrud.exception;

import java.time.LocalDateTime;

/**
 * Resposta de erro padrão exibida na página de erro.
 *
 * @param status    código HTTP (ex.: 404)
 * @param error     descrição curta do código HTTP (ex.: "Not Found")
 * @param message   mensagem compreensível para o usuário
 * @param path      endereço que originou o erro
 * @param timestamp momento em que o erro ocorreu
 */
public record ErrorDetails(int status, String error, String message, String path, LocalDateTime timestamp) {
}
