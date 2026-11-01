package com.quimia.quimiawebcrud.service.verification;

import org.junit.jupiter.api.Test;

import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A leitura dos registros MX (sem consultar o DNS de verdade).
 */
class EmailDomainLookupTest {

    private BasicAttribute mx(String... records) {
        BasicAttribute attribute = new BasicAttribute("MX");
        for (String record : records) {
            attribute.add(record);
        }
        return attribute;
    }

    @Test
    void aDomainWithAMailServerAcceptsMail() throws NamingException {
        assertTrue(EmailDomainLookup.acceptsMail(mx("10 alt1.gmail-smtp-in.l.google.com.")));
        assertTrue(EmailDomainLookup.acceptsMail(mx("30 mx2.exemplo.com.", "10 mx1.exemplo.com.")));
    }

    @Test
    void theNullMxMeansTheDomainDoesNotReceiveMail() throws NamingException {
        assertFalse(EmailDomainLookup.acceptsMail(mx("0 .")));
        assertFalse(EmailDomainLookup.acceptsMail(mx(".")));
    }

    @Test
    void aNullMxMixedWithARealOneStillAcceptsMail() throws NamingException {
        assertTrue(EmailDomainLookup.acceptsMail(mx("0 .", "10 mx1.exemplo.com.")));
    }

    @Test
    void anEmptyListOfRecordsDoesNotAcceptMail() throws NamingException {
        assertFalse(EmailDomainLookup.acceptsMail(mx()));
    }
}
