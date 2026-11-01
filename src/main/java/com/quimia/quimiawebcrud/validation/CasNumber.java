package com.quimia.quimiawebcrud.validation;

import java.util.regex.Pattern;

/**
 * Conferência do número CAS (Chemical Abstracts Service) pelo dígito verificador: os números antes do último hífen,
 * da direita para a esquerda, são multiplicados por 1, 2, 3... e a soma, dividida por 10, deixa como resto o último
 * dígito. Exemplo, 7732-18-5 (água): 8*1 + 1*2 + 2*3 + 3*4 + 7*5 + 7*6 = 105, e o resto de 105 por 10 é 5.
 * Só confere se o número é possível; se a substância existe é outra conferência ({@code ExternalChecks}).
 */
public final class CasNumber {

    private static final Pattern FORMAT = Pattern.compile("(\\d{2,7})-(\\d{2})-(\\d)");

    private CasNumber() {
    }

    /** Formato 0000000-00-0 e dígito verificador certo. */
    public static boolean isValid(String cas) {
        if (cas == null) {
            return false;
        }
        var matcher = FORMAT.matcher(cas);
        if (!matcher.matches()) {
            return false;
        }
        String digits = matcher.group(1) + matcher.group(2);
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int fromTheRight = digits.length() - i;
            sum += (digits.charAt(i) - '0') * fromTheRight;
        }
        return sum % 10 == Integer.parseInt(matcher.group(3));
    }
}
