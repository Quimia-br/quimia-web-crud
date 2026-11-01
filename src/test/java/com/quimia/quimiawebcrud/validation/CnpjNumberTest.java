package com.quimia.quimiawebcrud.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A conta dos dígitos verificadores do CNPJ.
 */
class CnpjNumberTest {

    @Test
    void realCnpjsWithTheRightCheckDigitsAreValid() {
        assertTrue(CnpjNumber.isValid("00000000000191"), "Banco do Brasil");
        assertTrue(CnpjNumber.isValid("11222333000181"));
        assertTrue(CnpjNumber.isValid("12345678000195"));
    }

    @Test
    void aWrongCheckDigitIsRefused() {
        assertFalse(CnpjNumber.isValid("00000000000192"));
        assertFalse(CnpjNumber.isValid("12345678000190"));
        assertFalse(CnpjNumber.isValid("11222333000182"));
    }

    @Test
    void anyChangedDigitBreaksTheNumber() {
        String valid = "11222333000181";
        for (int i = 0; i < valid.length(); i++) {
            char changed = valid.charAt(i) == '9' ? '0' : (char) (valid.charAt(i) + 1);
            String forged = valid.substring(0, i) + changed + valid.substring(i + 1);
            assertFalse(CnpjNumber.isValid(forged), "posição " + i + ": " + forged);
        }
    }

    @Test
    void sequencesOfTheSameDigitAreRefusedEvenThoughTheyPassTheMath() {
        for (char digit = '0'; digit <= '9'; digit++) {
            assertFalse(CnpjNumber.isValid(String.valueOf(digit).repeat(14)), "só " + digit);
        }
    }

    @Test
    void wrongSizeLettersPunctuationAndNullAreRefused() {
        assertFalse(CnpjNumber.isValid(null));
        assertFalse(CnpjNumber.isValid(""));
        assertFalse(CnpjNumber.isValid("1122233300018"));
        assertFalse(CnpjNumber.isValid("112223330001810"));
        assertFalse(CnpjNumber.isValid("11.222.333/0001-81"));
        assertFalse(CnpjNumber.isValid("1122233300018A"));
        assertFalse(CnpjNumber.isValid(" 1122233300018"));
    }
}
