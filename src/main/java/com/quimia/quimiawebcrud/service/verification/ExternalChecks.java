package com.quimia.quimiawebcrud.service.verification;

import com.quimia.quimiawebcrud.exception.InvalidDataException;
import lombok.extern.slf4j.Slf4j;

import java.net.IDN;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Confere em fontes externas se o e-mail, o CNPJ e o número CAS de um cadastro existem de verdade:
 * <ul>
 *   <li>e-mail: o domínio existe e recebe mensagens (DNS);</li>
 *   <li>CNPJ: a empresa está na base da Receita Federal (BrasilAPI);</li>
 *   <li>CAS: a substância está no PubChem.</li>
 * </ul>
 * Só barra quando a fonte responde com certeza que NÃO existe. Fonte fora do ar ou lenta deixa o cadastro seguir
 * (a falha não é do dado), e o valor que não mudou na edição nem é conferido de novo. As respostas ficam guardadas
 * por um tempo na memória para o mesmo valor não ser consultado várias vezes. Cada conferência pode ser desligada
 * pelas propriedades {@code app.verify.*}.
 *
 * <p>Essas consultas são lentas (até alguns segundos) e por isso rodam ANTES da transação do cadastro
 * ({@code CrudService#verifyBeforeSave}): numa transação, segurariam uma das poucas conexões do banco.
 */
@Slf4j
public class ExternalChecks {

    static final Duration KEEP_FOUND = Duration.ofHours(6);
    static final Duration KEEP_NOT_FOUND = Duration.ofMinutes(10);
    private static final int MAX_CACHED = 2_000;
    private static final Pattern DOMAIN = Pattern.compile("^(?=.{1,253}$)([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$");
    private static final Pattern CNPJ_DIGITS = Pattern.compile("\\d{14}");
    private static final Pattern CAS = Pattern.compile("\\d{2,7}-\\d{2}-\\d");

    private record Cached(LookupResult result, Instant expiresAt) {
    }

    private final Lookup email;
    private final Lookup cnpj;
    private final Lookup cas;
    private final boolean checkEmail;
    private final boolean checkCnpj;
    private final boolean checkCas;
    private final Clock clock;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public ExternalChecks(Lookup email, Lookup cnpj, Lookup cas, boolean checkEmail, boolean checkCnpj, boolean checkCas,
                          Clock clock) {
        this.email = email;
        this.cnpj = cnpj;
        this.cas = cas;
        this.checkEmail = checkEmail;
        this.checkCnpj = checkCnpj;
        this.checkCas = checkCas;
        this.clock = clock;
    }

    /**
     * O domínio do e-mail precisa existir e receber mensagens. {@code current} é o e-mail que o registro já tem
     * (nulo na criação): se não mudou, não há o que conferir.
     */
    public void requireEmailDomain(String newEmail, String current) {
        if (!checkEmail || isBlank(newEmail) || sameValue(newEmail, current)) {
            return;
        }
        String domain = domainOf(newEmail);
        if (domain == null) {
            return;
        }
        if (lookup("email:" + domain, () -> email.check(domain)) == LookupResult.NOT_FOUND) {
            throw new InvalidDataException("Não foi possível confirmar o e-mail: o domínio \"" + domain
                    + "\" não existe ou não recebe mensagens. Confira o endereço.");
        }
    }

    /** A empresa precisa estar na base da Receita Federal. */
    public void requireCnpj(String newCnpj, String current) {
        if (!checkCnpj || isBlank(newCnpj) || sameValue(newCnpj, current) || !CNPJ_DIGITS.matcher(newCnpj).matches()) {
            return;
        }
        if (lookup("cnpj:" + newCnpj, () -> cnpj.check(newCnpj)) == LookupResult.NOT_FOUND) {
            throw new InvalidDataException("Este CNPJ não foi encontrado na base da Receita Federal. Confira os números.");
        }
    }

    /** A substância precisa estar no PubChem. */
    public void requireCas(String newCas, String current) {
        if (!checkCas || isBlank(newCas) || sameValue(newCas, current) || !CAS.matcher(newCas).matches()) {
            return;
        }
        if (lookup("cas:" + newCas, () -> cas.check(newCas)) == LookupResult.NOT_FOUND) {
            throw new InvalidDataException("Este número CAS não foi encontrado no PubChem. Confira os números.");
        }
    }

    /** O que está depois do último @, em minúsculas e em ASCII; nulo se não parece um domínio. */
    static String domainOf(String address) {
        int at = address.lastIndexOf('@');
        if (at < 0) {
            return null;
        }
        String domain = address.substring(at + 1).trim().toLowerCase(Locale.ROOT);
        try {
            domain = IDN.toASCII(domain);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return DOMAIN.matcher(domain).matches() ? domain : null;
    }

    private LookupResult lookup(String key, Supplier<LookupResult> source) {
        Instant now = clock.instant();
        Cached cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.result();
        }
        LookupResult result = source.get();
        if (result != LookupResult.UNKNOWN) {
            remember(key, result, now);
        }
        return result;
    }

    private void remember(String key, LookupResult result, Instant now) {
        if (cache.size() >= MAX_CACHED) {
            cache.values().removeIf(entry -> !entry.expiresAt().isAfter(now));
            if (cache.size() >= MAX_CACHED) {
                cache.clear();
            }
        }
        Duration keep = result == LookupResult.FOUND ? KEEP_FOUND : KEEP_NOT_FOUND;
        cache.put(key, new Cached(result, now.plus(keep)));
    }

    private boolean sameValue(String a, String b) {
        return b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
