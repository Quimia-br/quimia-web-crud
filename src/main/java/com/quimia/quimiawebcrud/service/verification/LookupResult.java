package com.quimia.quimiawebcrud.service.verification;

/**
 * O que uma consulta externa respondeu. {@code UNKNOWN} é a fonte fora do ar, lenta ou recusando o pedido:
 * nesse caso o cadastro não é barrado, porque a falha não é do dado.
 */
public enum LookupResult {
    FOUND,
    NOT_FOUND,
    UNKNOWN
}
