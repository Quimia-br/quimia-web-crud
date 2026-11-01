package com.quimia.quimiawebcrud.service.verification;

/**
 * Uma consulta a uma fonte externa (DNS, Receita Federal, PubChem). Nunca lança exceção: qualquer falha de rede
 * vira {@link LookupResult#UNKNOWN}.
 */
@FunctionalInterface
public interface Lookup {

    LookupResult check(String value);
}
