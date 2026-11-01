package com.quimia.quimiawebcrud;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os textos em português e em inglês andam juntos: mesmas chaves, mesmos espaços para valores, nada em branco, e toda
 * chave usada num template ou no serviço do dashboard existe (e nenhuma sobra sem uso).
 */
class MessagesTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path JAVA = Path.of("src/main/java");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");
    private static final Pattern IN_TEMPLATE = Pattern.compile("(?:#\\{|msg\\(')([A-Za-z0-9_.\\-]+)");
    private static final Pattern IN_JAVA = Pattern.compile("\"((?:dash|nav|settings|login|lang)\\.[A-Za-z0-9_.\\-]+)\"");

    private final Properties portuguese = load("/messages.properties");
    private final Properties english = load("/messages_en.properties");

    private Properties load(String resource) {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return properties;
    }

    @Test
    void bothLanguagesHaveTheSameKeys() {
        Set<String> onlyPt = new TreeSet<>(portuguese.stringPropertyNames());
        onlyPt.removeAll(english.stringPropertyNames());
        Set<String> onlyEn = new TreeSet<>(english.stringPropertyNames());
        onlyEn.removeAll(portuguese.stringPropertyNames());

        assertTrue(onlyPt.isEmpty(), "só em português: " + onlyPt);
        assertTrue(onlyEn.isEmpty(), "só em inglês: " + onlyEn);
    }

    @Test
    void noTextIsBlankAndEachKeyKeepsTheSamePlaceholders() {
        for (String key : portuguese.stringPropertyNames()) {
            String pt = portuguese.getProperty(key);
            String en = english.getProperty(key);
            assertFalse(pt.isBlank() || en.isBlank(), "texto em branco: " + key);
            assertEquals(placeholders(pt), placeholders(en), "os {0}, {1}... de " + key + " precisam ser os mesmos nas duas línguas");
        }
    }

    @Test
    void englishIsReallyTranslatedAndNotACopyOfPortuguese() {
        // só estes textos são iguais de verdade nas duas línguas (nome de idioma, palavra igual)
        Set<String> sameInBoth = Set.of("dash.pageTitle", "lang.en", "lang.pt-BR", "login.email", "nav.dashboard");
        for (String key : portuguese.stringPropertyNames()) {
            boolean same = portuguese.getProperty(key).equals(english.getProperty(key));
            assertEquals(sameInBoth.contains(key), same, key + (same ? " está igual nas duas línguas (esqueceu de traduzir?)" : " deveria ser igual nas duas"));
        }
    }

    @Test
    void everyKeyUsedByATemplateOrByTheDashboardServiceExists() throws IOException {
        Set<String> used = usedKeys();

        Set<String> missing = new TreeSet<>(used);
        missing.removeAll(portuguese.stringPropertyNames());

        assertTrue(missing.isEmpty(), "chaves usadas e que não existem: " + missing);
        assertFalse(used.isEmpty());
    }

    @Test
    void noKeyIsLeftUnused() throws IOException {
        Set<String> unused = new TreeSet<>(portuguese.stringPropertyNames());
        unused.removeAll(usedKeys());

        assertTrue(unused.isEmpty(), "chaves que nenhum template ou serviço usa: " + unused);
    }

    private Set<String> placeholders(String text) {
        Set<String> numbers = new HashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group(1));
        }
        return numbers;
    }

    private Set<String> usedKeys() throws IOException {
        Set<String> used = new HashSet<>();
        collect(TEMPLATES, ".html", IN_TEMPLATE, used);
        collect(JAVA, ".java", IN_JAVA, used);
        return used;
    }

    private void collect(Path root, String extension, Pattern pattern, Set<String> into) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(extension))::iterator) {
                Matcher matcher = pattern.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    into.add(matcher.group(1));
                }
            }
        }
    }

    @Test
    void theMessageFilesAreUtf8WithRealAccents() {
        assertTrue(portuguese.getProperty("login.error").contains("inválidos"));
        assertTrue(portuguese.getProperty("settings.title").contains("ç"));
    }
}
