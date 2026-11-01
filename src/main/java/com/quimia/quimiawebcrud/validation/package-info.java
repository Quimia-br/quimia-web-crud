/**
 * Tudo o que valida um formulário sem consultar nada fora do app: as expressões regulares (FormPatterns) e os dígitos
 * verificadores do CNPJ e do número CAS, usados nos formulários como as anotações {@code @ValidCnpj} e {@code @ValidCas}.
 * A conferência que depende de rede (DNS, BrasilAPI, PubChem) fica em service.verification.
 */
package com.quimia.quimiawebcrud.validation;
