package org.example.heuristica;

import org.example.modelo.ObjetoFixo;
import org.example.modelo.Percecao;

import java.util.ArrayList;
import java.util.List;

// Cérebro determinístico. Decide a ação SOZINHO. Não faz HTTP/UI/Ollama.
public class MotorHeuristico {

    // As quatro intenções base. Confirmadas no Swagger.
    private static final String[] INTENCOES = {
            "MOVER_NORTE", "MOVER_SUL", "MOVER_ESTE", "MOVER_OESTE"
    };

    // Recebe a perceção e devolve a ação a executar. Por agora: primeira válida.
    public String decidirAcao(Percecao p) {
        List<String> validas = filtrarColisoes(p);
        if (validas.isEmpty()) {
            // Cercado por muros — fica parado enviando um movimento qualquer
            // (o servidor bloqueia, mas não crasha). Revisto na Fase 3.
            return INTENCOES[0];
        }
        return validas.get(0);
    }

    // Remove as intenções cujo destino colide com um muro (objetos_fixos).
    private List<String> filtrarColisoes(Percecao p) {
        int x = p.getO_meu_estado().getX();
        int y = p.getO_meu_estado().getY();
        List<String> validas = new ArrayList<>();

        for (String intencao : INTENCOES) {
            int destinoX = x;
            int destinoY = y;
            switch (intencao) {
                case "MOVER_NORTE": destinoY = y - 1; break;
                case "MOVER_SUL":   destinoY = y + 1; break;
                case "MOVER_ESTE":  destinoX = x + 1; break;
                case "MOVER_OESTE": destinoX = x - 1; break;
            }
            if (!haMuro(p, destinoX, destinoY)) {
                validas.add(intencao);
            }
        }
        return validas;
    }

    // True se algum objeto_fixo ocupa a coordenada dada.
    private boolean haMuro(Percecao p, int x, int y) {
        if (p.getObjetos_fixos() == null) return false;
        for (ObjetoFixo muro : p.getObjetos_fixos()) {
            if (muro.getX() == x && muro.getY() == y) {
                return true;
            }
        }
        return false;
    }
}