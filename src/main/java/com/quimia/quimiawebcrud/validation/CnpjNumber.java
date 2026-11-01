package com.quimia.quimiawebcrud.validation;

/**
 * Conferência do CNPJ pelos dois dígitos verificadores (módulo 11), a mesma conta que a Receita Federal usa.
 * Só confere se o número é matematicamente possível; se a empresa existe é outra conferência
 * ({@code ExternalChecks}).
 */
public final class CnpjNumber {

    private static final int LENGTH = 14;
    private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjNumber() {
    }

    /** 14 números, não todos iguais (00000000000000 passaria na conta) e com os dois dígitos finais certos. */
    public static boolean isValid(String cnpj) {
        if (cnpj == null || cnpj.length() != LENGTH || !cnpj.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return false;
        }
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int first = checkDigit(cnpj, FIRST_WEIGHTS);
        int second = checkDigit(cnpj, SECOND_WEIGHTS);
        return first == cnpj.charAt(12) - '0' && second == cnpj.charAt(13) - '0';
    }

    /** Soma cada dígito vezes o seu peso; o resto da divisão por 11 dá o dígito (resto menor que 2 vira 0). */
    private static int checkDigit(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (cnpj.charAt(i) - '0') * weights[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }
}
