package com.quimia.quimiawebcrud.dto;

/**
 * Expressões usadas na validação dos formulários. Os templates repetem as mesmas regras nos atributos
 * "pattern" para o navegador avisar na hora; aqui fica a versão que vale de verdade, no servidor.
 */
final class FormPatterns {

    /** Texto@domínio.sufixo: exige ponto no domínio (nome@dominio.com). */
    static final String EMAIL = "^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$";

    /** Vazio mantém a senha atual na edição; senão de 8 a 72 caracteres. */
    static final String PASSWORD = "^$|.{8,72}";

    static final String CNPJ = "\\d{14}";

    /** Número CAS, por exemplo 7681-52-9. */
    static final String CAS = "\\d{2,7}-\\d{2}-\\d";

    private FormPatterns() {
    }
}
