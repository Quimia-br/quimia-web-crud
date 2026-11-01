package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.config.LocaleConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Configurações do painel que dependem do servidor: o idioma. (O tema claro ou escuro fica só no navegador, em theme.js.)
 */
@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final LocaleResolver localeResolver;

    /**
     * Guarda o idioma escolhido num cookie e volta para a tela de onde o pedido veio. Só valem os idiomas oferecidos;
     * qualquer outro valor vira o português.
     */
    @PostMapping("/language")
    public String language(@RequestParam("lang") String lang, HttpServletRequest request, HttpServletResponse response) {
        localeResolver.setLocale(request, response, LocaleConfig.supported(lang));
        return "redirect:" + backTo(request);
    }

    /**
     * O endereço de onde o pedido veio (cabeçalho Referer), mas só se for do próprio painel: caminho e parâmetros, sem
     * servidor. Qualquer outra coisa (de outro site, em branco, malformada) leva para a tela inicial, para a rota
     * nunca servir de redirecionamento para fora.
     */
    static String backTo(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = new URI(referer);
            boolean web = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            boolean sameServer = web && uri.getHost() != null && uri.getHost().equalsIgnoreCase(request.getServerName());
            String context = request.getContextPath();
            String path = uri.getRawPath();
            if (!sameServer || path == null || !path.startsWith(context + "/") && !path.equals(context)) {
                return "/";
            }
            String inside = path.substring(context.length());
            if (inside.isEmpty() || inside.startsWith("//") || inside.contains("\\")) {
                return "/";
            }
            return uri.getRawQuery() == null ? inside : inside + "?" + uri.getRawQuery();
        } catch (URISyntaxException e) {
            return "/";
        }
    }
}
