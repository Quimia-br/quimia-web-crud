package com.quimia.quimiawebcrud.service.verification;

import com.quimia.quimiawebcrud.exception.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * As conferências externas de e-mail, CNPJ e CAS: só barram quando a fonte diz que NÃO existe, não consultam o que não
 * mudou e guardam as respostas por um tempo. As fontes aqui são falsas: nenhum teste vai à internet.
 */
class ExternalChecksTest {

    /** Relógio que só anda quando o teste manda. */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-01T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** Fonte falsa: responde sempre o mesmo e anota o que lhe perguntaram. */
    private static class FakeSource implements Lookup {
        LookupResult answer;
        final List<String> asked = new ArrayList<>();

        FakeSource(LookupResult answer) {
            this.answer = answer;
        }

        @Override
        public LookupResult check(String value) {
            asked.add(value);
            return answer;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final FakeSource email = new FakeSource(LookupResult.FOUND);
    private final FakeSource cnpj = new FakeSource(LookupResult.FOUND);
    private final FakeSource cas = new FakeSource(LookupResult.FOUND);
    private final ExternalChecks checks = new ExternalChecks(email, cnpj, cas, true, true, true, clock);

    // ---- e-mail

    @Test
    void anEmailWhoseDomainExistsPasses() {
        assertDoesNotThrow(() -> checks.requireEmailDomain("ana@Gmail.com", null));
        assertEquals(List.of("gmail.com"), email.asked, "só o domínio, em minúsculas, vai à consulta");
    }

    @Test
    void anEmailWhoseDomainDoesNotExistIsRefusedWithTheDomainInTheMessage() {
        email.answer = LookupResult.NOT_FOUND;

        InvalidDataException error = assertThrows(InvalidDataException.class,
                () -> checks.requireEmailDomain("ana@dominio-que-nao-existe.com", null));

        assertTrue(error.getMessage().contains("dominio-que-nao-existe.com"), error.getMessage());
    }

    @Test
    void aSourceThatDoesNotAnswerDoesNotBlockTheEmail() {
        email.answer = LookupResult.UNKNOWN;

        assertDoesNotThrow(() -> checks.requireEmailDomain("ana@gmail.com", null));
    }

    @Test
    void anUnchangedEmailIsNotCheckedAgainIgnoringCaseAndSpaces() {
        email.answer = LookupResult.NOT_FOUND;

        assertDoesNotThrow(() -> checks.requireEmailDomain("Ana@Gmail.com ", "ana@gmail.com"));
        assertTrue(email.asked.isEmpty());
    }

    @Test
    void anEditedEmailIsCheckedAgain() {
        checks.requireEmailDomain("outro@gmail.com", "ana@gmail.com");

        assertEquals(List.of("gmail.com"), email.asked);
    }

    @Test
    void domainsAreExtractedFromAfterTheLastAtSign() {
        assertEquals("gmail.com", ExternalChecks.domainOf("ana@gmail.com"));
        assertEquals("exemplo.com.br", ExternalChecks.domainOf("a@b@Exemplo.com.br"));
        assertEquals("xn--bcher-kva.de", ExternalChecks.domainOf("ana@bücher.de"), "domínio com acento vira ASCII");
        assertNull(ExternalChecks.domainOf("sem-arroba"));
        assertNull(ExternalChecks.domainOf("ana@semponto"));
        assertNull(ExternalChecks.domainOf("ana@-ruim-.com"));
        assertNull(ExternalChecks.domainOf("ana@"));
        assertNull(ExternalChecks.domainOf("ana@exemplo.c"));
        assertNull(ExternalChecks.domainOf("ana@" + "a".repeat(64) + ".com"));
    }

    @Test
    void somethingThatDoesNotLookLikeADomainIsNeverSentToTheSource() {
        email.answer = LookupResult.NOT_FOUND;

        assertDoesNotThrow(() -> checks.requireEmailDomain("ana@semponto", null));
        assertDoesNotThrow(() -> checks.requireEmailDomain("ana@exe mplo.com", null));
        assertTrue(email.asked.isEmpty());
    }

    // ---- CNPJ

    @Test
    void aCnpjFoundInTheRegistryPasses() {
        assertDoesNotThrow(() -> checks.requireCnpj("11222333000181", null));
        assertEquals(List.of("11222333000181"), cnpj.asked);
    }

    @Test
    void aCnpjNotInTheRegistryIsRefused() {
        cnpj.answer = LookupResult.NOT_FOUND;

        InvalidDataException error = assertThrows(InvalidDataException.class, () -> checks.requireCnpj("98765432000198", null));

        assertTrue(error.getMessage().contains("Receita Federal"), error.getMessage());
    }

    @Test
    void aRegistryThatDoesNotAnswerDoesNotBlockTheCnpj() {
        cnpj.answer = LookupResult.UNKNOWN;

        assertDoesNotThrow(() -> checks.requireCnpj("98765432000198", null));
    }

    @Test
    void anUnchangedCnpjIsNotCheckedAgain() {
        cnpj.answer = LookupResult.NOT_FOUND;

        assertDoesNotThrow(() -> checks.requireCnpj("98765432000198", "98765432000198"));
        assertTrue(cnpj.asked.isEmpty());
    }

    @Test
    void aCnpjThatIsNotFourteenDigitsIsNeverSentToTheSource() {
        cnpj.answer = LookupResult.NOT_FOUND;

        assertDoesNotThrow(() -> checks.requireCnpj("../etc/passwd", null));
        assertDoesNotThrow(() -> checks.requireCnpj("1234", null));
        assertTrue(cnpj.asked.isEmpty());
    }

    // ---- CAS

    @Test
    void aSubstanceFoundInPubChemPasses() {
        assertDoesNotThrow(() -> checks.requireCas("7732-18-5", null));
        assertEquals(List.of("7732-18-5"), cas.asked);
    }

    @Test
    void aSubstanceNotInPubChemIsRefused() {
        cas.answer = LookupResult.NOT_FOUND;

        InvalidDataException error = assertThrows(InvalidDataException.class, () -> checks.requireCas("1234-56-6", null));

        assertTrue(error.getMessage().contains("PubChem"), error.getMessage());
    }

    @Test
    void aSourceThatDoesNotAnswerDoesNotBlockTheCas() {
        cas.answer = LookupResult.UNKNOWN;

        assertDoesNotThrow(() -> checks.requireCas("7732-18-5", null));
    }

    @Test
    void anUnchangedCasIsNotCheckedAgain() {
        cas.answer = LookupResult.NOT_FOUND;

        assertDoesNotThrow(() -> checks.requireCas("7732-18-5", "7732-18-5"));
        assertTrue(cas.asked.isEmpty());
    }

    @Test
    void aCasWithAnotherFormatIsNeverSentToTheSource() {
        cas.answer = LookupResult.NOT_FOUND;

        assertDoesNotThrow(() -> checks.requireCas("7732185", null));
        assertDoesNotThrow(() -> checks.requireCas("7732-18-5/../x", null));
        assertTrue(cas.asked.isEmpty());
    }

    // ---- ligar e desligar, vazio e memória

    @Test
    void eachCheckCanBeSwitchedOff() {
        email.answer = LookupResult.NOT_FOUND;
        cnpj.answer = LookupResult.NOT_FOUND;
        cas.answer = LookupResult.NOT_FOUND;
        ExternalChecks off = new ExternalChecks(email, cnpj, cas, false, false, false, clock);

        assertDoesNotThrow(() -> off.requireEmailDomain("ana@dominio-que-nao-existe.com", null));
        assertDoesNotThrow(() -> off.requireCnpj("98765432000198", null));
        assertDoesNotThrow(() -> off.requireCas("1234-56-6", null));
        assertTrue(email.asked.isEmpty() && cnpj.asked.isEmpty() && cas.asked.isEmpty());
    }

    @Test
    void blankAndNullValuesAreIgnored() {
        email.answer = LookupResult.NOT_FOUND;

        assertDoesNotThrow(() -> checks.requireEmailDomain(null, null));
        assertDoesNotThrow(() -> checks.requireEmailDomain("  ", null));
        assertDoesNotThrow(() -> checks.requireCnpj(null, null));
        assertDoesNotThrow(() -> checks.requireCas("", null));
        assertTrue(email.asked.isEmpty() && cnpj.asked.isEmpty() && cas.asked.isEmpty());
    }

    @Test
    void theSameValueIsOnlyAskedOnceWhileTheAnswerIsKept() {
        checks.requireCnpj("11222333000181", null);
        checks.requireCnpj("11222333000181", "outro");
        checks.requireCnpj("11222333000181", null);

        assertEquals(1, cnpj.asked.size());
    }

    @Test
    void aFoundAnswerIsKeptForHoursAndThenAskedAgain() {
        checks.requireCnpj("11222333000181", null);
        clock.advance(ExternalChecks.KEEP_FOUND.minusMinutes(1));
        checks.requireCnpj("11222333000181", null);
        assertEquals(1, cnpj.asked.size());

        clock.advance(Duration.ofMinutes(2));
        checks.requireCnpj("11222333000181", null);
        assertEquals(2, cnpj.asked.size());
    }

    @Test
    void aNotFoundAnswerIsKeptOnlyBrieflySoACorrectionIsSeenSoon() {
        cnpj.answer = LookupResult.NOT_FOUND;
        assertThrows(InvalidDataException.class, () -> checks.requireCnpj("98765432000198", null));
        assertThrows(InvalidDataException.class, () -> checks.requireCnpj("98765432000198", null));
        assertEquals(1, cnpj.asked.size());

        clock.advance(ExternalChecks.KEEP_NOT_FOUND.plusSeconds(1));
        cnpj.answer = LookupResult.FOUND;
        assertDoesNotThrow(() -> checks.requireCnpj("98765432000198", null));
        assertEquals(2, cnpj.asked.size());
    }

    @Test
    void anUnknownAnswerIsNeverKept() {
        cnpj.answer = LookupResult.UNKNOWN;
        checks.requireCnpj("11222333000181", null);
        checks.requireCnpj("11222333000181", null);

        assertEquals(2, cnpj.asked.size());
    }
}
