package com.quimia.quimiawebcrud.service.verification;

import lombok.extern.slf4j.Slf4j;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * Confere se o domínio de um e-mail existe e recebe mensagens, pelo DNS: procura os servidores de e-mail (registro MX)
 * e, se não houver, o endereço do próprio domínio (A ou AAAA), como manda o protocolo de e-mail. Só o domínio vai ao
 * DNS, nunca o e-mail inteiro. Não dá para saber se a CAIXA existe (isso só um e-mail de confirmação mostra):
 * o que se descobre é se {@code nome@dominio.com} pode, ao menos, receber algo.
 */
@Slf4j
public class EmailDomainLookup implements Lookup {

    private final int timeoutMillis;

    public EmailDomainLookup(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public LookupResult check(String domain) {
        DirContext context = null;
        try {
            context = new InitialDirContext(environment());
            Attribute mx = context.getAttributes(domain, new String[]{"MX"}).get("MX");
            if (mx != null && mx.size() > 0) {
                return acceptsMail(mx) ? LookupResult.FOUND : LookupResult.NOT_FOUND;
            }
            // Um tipo por vez: pedir "A" e "AAAA" juntos devolve vazio no resolvedor do JNDI (testado com o DNS de verdade)
            for (String type : new String[]{"A", "AAAA"}) {
                if (context.getAttributes(domain, new String[]{type}).size() > 0) {
                    return LookupResult.FOUND;
                }
            }
            return LookupResult.NOT_FOUND;
        } catch (NameNotFoundException e) {
            return LookupResult.NOT_FOUND;
        } catch (NamingException e) {
            log.warn("O DNS não respondeu ao conferir um domínio de e-mail ({}); o cadastro segue sem a conferência", e.toString());
            return LookupResult.UNKNOWN;
        } finally {
            close(context);
        }
    }

    /**
     * Algum servidor de e-mail de verdade? O registro "0 ." (MX nulo, RFC 7505) é o jeito de um domínio dizer
     * que não recebe e-mail nenhum.
     */
    static boolean acceptsMail(Attribute mx) throws NamingException {
        NamingEnumeration<?> records = mx.getAll();
        while (records.hasMore()) {
            String[] parts = records.next().toString().trim().split("\\s+");
            String target = parts[parts.length - 1];
            if (!target.equals(".") && !target.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Hashtable<String, String> environment() {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns:");
        env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(timeoutMillis));
        env.put("com.sun.jndi.dns.timeout.retries", "1");
        return env;
    }

    private void close(DirContext context) {
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (NamingException e) {
            log.debug("Não foi possível fechar o contexto de DNS", e);
        }
    }
}
