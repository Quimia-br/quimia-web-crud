package com.quimia.quimiawebcrud;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O tema escuro: o CSS troca todas as cores de uma vez só por variáveis, e o theme.js escolhe o tema antes da página
 * aparecer. Estes testes impedem que uma cor nova entre no tema claro e fique esquecida no escuro.
 */
class ThemeAssetsTest {

    private static final Path STATIC = Path.of("src/main/resources/static");
    private static final Pattern COLOR_VARIABLE = Pattern.compile("^\\s*(--color-[a-z-]+):", Pattern.MULTILINE);
    /** Iguais nos dois temas de propósito: texto sobre o menu e sobre botões de cor cheia. */
    private static final Set<String> SAME_IN_BOTH = Set.of("--color-sidebar-text", "--color-on-primary");

    private String read(String file) throws IOException {
        return Files.readString(STATIC.resolve(file), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private Set<String> colorVariables(String block) {
        Set<String> names = new TreeSet<>();
        Matcher matcher = COLOR_VARIABLE.matcher(block);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private String lightBlock(String css) {
        int start = css.indexOf(":root {");
        return css.substring(start, css.indexOf("\n}\n", start));
    }

    private String darkBlock(String css) {
        int start = css.indexOf(":root[data-theme=\"dark\"] {");
        return css.substring(start, css.indexOf("\n}\n", start));
    }

    @Test
    void theDarkThemeOverridesEveryColorVariableOfTheLightTheme() throws IOException {
        String css = read("css/style.css");
        Set<String> missing = colorVariables(lightBlock(css));
        missing.removeAll(colorVariables(darkBlock(css)));
        missing.removeAll(SAME_IN_BOTH);

        assertTrue(missing.isEmpty(), "variáveis de cor sem versão escura: " + missing);
    }

    @Test
    void theDarkThemeDoesNotInventVariablesTheLightThemeLacks() throws IOException {
        String css = read("css/style.css");
        Set<String> extra = colorVariables(darkBlock(css));
        extra.removeAll(colorVariables(lightBlock(css)));

        assertTrue(extra.isEmpty(), "só no escuro: " + extra);
    }

    @Test
    void theDarkColorsAreDifferentFromTheLightOnes() throws IOException {
        String css = read("css/style.css");
        Pattern value = Pattern.compile("(--color-(?:bg|surface|text|border)):\\s*([^;]+);");
        Matcher light = value.matcher(lightBlock(css));
        String dark = darkBlock(css);
        int checked = 0;
        while (light.find()) {
            Matcher same = Pattern.compile(Pattern.quote(light.group(1)) + ":\\s*" + Pattern.quote(light.group(2)) + ";").matcher(dark);
            assertFalse(same.find(), light.group(1) + " é igual nos dois temas");
            checked++;
        }
        assertTrue(checked >= 4);
    }

    @Test
    void theDashboardChartsHaveTheirOwnDarkColorsAndTheLoginHasNone() throws IOException {
        assertTrue(read("css/dashboard.css").contains(":root[data-theme=\"dark\"] .dashboard"));
        assertFalse(read("css/auth.css").contains("data-theme"), "o login é sempre claro");
    }

    @Test
    void themeScriptKeepsTheChoiceInTheBrowserAndFollowsTheSystemByDefault() throws IOException {
        String js = read("js/theme.js");

        assertTrue(js.contains("quimia.theme") && js.contains("localStorage"));
        assertTrue(js.contains("prefers-color-scheme: dark"), "sem escolha, acompanha o sistema");
        assertTrue(js.contains("data-theme") && js.contains("data-theme-option"));
        assertTrue(js.contains("'light'") && js.contains("'dark'") && js.contains("'system'"));
        assertFalse(js.contains("eval(") || js.contains("innerHTML"), "nada de código dinâmico");
    }

    @Test
    void theContentIconsGetLightInTheDarkTheme() throws IOException {
        String css = read("css/style.css");

        assertTrue(css.contains(":root[data-theme=\"dark\"] .btn-icon img"));
        assertTrue(css.contains("filter: invert(1) hue-rotate(180deg)"));
    }

    @Test
    void theDashboardLayoutFollowsTheWidthOfTheContentAndNotOfTheWindow() throws IOException {
        String css = read("css/dashboard.css");

        assertTrue(css.contains("container: dashboard / inline-size"), "o conteúdo é o contêiner");
        assertTrue(css.contains("@container dashboard (max-width:"), "as colunas respondem ao contêiner");
        assertFalse(css.contains("@media (max-width: 1100px)"), "a janela não decide mais as colunas do dashboard");
        assertTrue(css.contains(".dash-welcome {\n  flex: 1 1 300px;\n  min-width: 0;"), "o título cede espaço em vez de empurrar as ferramentas para baixo");
    }
}
