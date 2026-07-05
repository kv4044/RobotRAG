package org.example.rag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExtratorChave {
    private ExtratorChave() {}

    // Códigos do manual: PALAVRA-SUFIXO (XTR-99, SIGMA-3, OPTIC-ZOOM, GROUND-0…).
    // Hífen é obrigatório e PRESERVADO — o servidor recebe o código literal (code=XTR-99).
    private static final Pattern CHAVE_PATTERN = Pattern.compile("[A-Z]{2,}(?:-[A-Z0-9]+)+");

    public static String extrair(String respostaBruta) {
        if (respostaBruta == null) return null;
        Matcher m = CHAVE_PATTERN.matcher(respostaBruta.toUpperCase());
        if (m.find()) return m.group(); // "NULL" não tem hífen -> nunca casa -> devolve null
        return null;
    }
}