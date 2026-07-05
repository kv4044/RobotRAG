package org.example.rag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExtratorChave {
    private ExtratorChave() {}

    private static final Pattern CHAVE_PATTERN = Pattern.compile("[A-Z]{2,}-?[A-Z0-9]{2,}");

    public static String extrair(String respostaBruta) {
        if (respostaBruta == null) return null;
        Matcher m = CHAVE_PATTERN.matcher(respostaBruta.toUpperCase());
        while (m.find()) {
            String candidata = m.group().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            if (!candidata.equals("NULL")) {
                return candidata;
            }
        }
        return null;
    }
}