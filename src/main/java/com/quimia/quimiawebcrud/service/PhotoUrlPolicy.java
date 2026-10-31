package com.quimia.quimiawebcrud.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Decide se um valor de photoUrl pode virar o "src" de uma imagem na tela.
 * Só passam endereços http(s) sem caracteres perigosos e o caminho das fotos salvas por este sistema;
 * qualquer outra coisa (ex.: "javascript:...", "data:...") é ignorada.
 */
@Component("photoUrlPolicy")
public class PhotoUrlPolicy {

    private static final Pattern SAFE = Pattern.compile(
            "^(https?://[^\\s\"'<>\\\\]{1,2000}|/uploads/photos/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg))$",
            Pattern.CASE_INSENSITIVE);

    /** Verdadeiro se o endereço pode ser usado como src de imagem. */
    public boolean isSafe(String url) {
        return url != null && SAFE.matcher(url).matches();
    }
}
