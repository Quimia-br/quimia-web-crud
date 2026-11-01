package com.quimia.quimiawebcrud.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O formato e o dígito verificador do número CAS.
 */
class CasNumberTest {

    @Test
    void wellKnownSubstancesAreValid() {
        assertTrue(CasNumber.isValid("7732-18-5"), "água");
        assertTrue(CasNumber.isValid("7681-52-9"), "hipoclorito de sódio");
        assertTrue(CasNumber.isValid("64-17-5"), "etanol");
        assertTrue(CasNumber.isValid("67-64-1"), "acetona");
        assertTrue(CasNumber.isValid("50-00-0"), "formaldeído");
        assertTrue(CasNumber.isValid("7647-01-0"), "ácido clorídrico");
    }

    @Test
    void aWrongCheckDigitIsRefused() {
        assertFalse(CasNumber.isValid("7732-18-4"));
        assertFalse(CasNumber.isValid("64-17-6"));
        assertFalse(CasNumber.isValid("1234-56-7"));
    }

    @Test
    void anyChangedDigitBeforeTheLastBreaksTheNumber() {
        String valid = "7732-18-5";
        for (int i : new int[]{0, 1, 2, 3, 5, 6}) {
            char changed = valid.charAt(i) == '9' ? '0' : (char) (valid.charAt(i) + 1);
            String forged = valid.substring(0, i) + changed + valid.substring(i + 1);
            assertFalse(CasNumber.isValid(forged), forged);
        }
    }

    @Test
    void wrongFormatsAreRefused() {
        assertFalse(CasNumber.isValid(null));
        assertFalse(CasNumber.isValid(""));
        assertFalse(CasNumber.isValid("7732185"));
        assertFalse(CasNumber.isValid("7732-18-55"));
        assertFalse(CasNumber.isValid("7-18-5"));
        assertFalse(CasNumber.isValid("77321234-18-5"));
        assertFalse(CasNumber.isValid("7732-1-85"));
        assertFalse(CasNumber.isValid("abcd-18-5"));
        assertFalse(CasNumber.isValid(" 7732-18-5"));
        assertFalse(CasNumber.isValid("7732-18-5\n"));
    }
}
