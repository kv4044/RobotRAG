package org.example.heuristica;

import org.example.modelo.ObjetoFixo;
import org.example.modelo.Percecao;
import org.example.ui.PainelMapaCalor;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

    // Cérebro determinístico. Decide a ação SOZINHO. Não faz HTTP/UI/Ollama.
    public class MotorHeuristico {

    // As quatro intenções base. Confirmadas no Swagger.
    private static final String[] INTENCOES = {
            "MOVER_NORTE", "MOVER_SUL", "MOVER_ESTE", "MOVER_OESTE"
    };

        // mapa de calor: chave "x,y" -> nº de vezes que o robô pisou essa coordenada
        private final Map<String, Integer> historicoVisitas = new HashMap<>();

        // constrói a chave textual da coordenada para o dicionário
        private String chave(int x, int y) {
            return x + "," + y;
        }

        // calcula a coordenada de destino de uma ação (mapeamento já confirmado na Fase 1+2)
        private int[] destino(String acao, int x, int y) {
            switch (acao) {
                case "MOVER_NORTE": return new int[]{x, y - 1};
                case "MOVER_SUL":   return new int[]{x, y + 1};
                case "MOVER_ESTE":  return new int[]{x + 1, y};
                case "MOVER_OESTE": return new int[]{x - 1, y};
                default:            return new int[]{x, y}; // ação inesperada: não desloca
            }
        }


        // Recebe a perceção e devolve a ação a executar. Por agora: primeira válida.
    public String decidirAcao(Percecao p) {
        List<String> validas = filtrarColisoes(p);
        if (validas.isEmpty()) {
            // Cercado por muros — fica parado enviando um movimento qualquer
            // (o servidor bloqueia, mas não crasha). Revisto na Fase 3.
            return INTENCOES[0];
        }
        int x = p.getO_meu_estado().getX();
        int y = p.getO_meu_estado().getY();

        // regista a passagem pela coordenada atual (mais visitas = bloco mais "quente")
        historicoVisitas.merge(chave(x, y), 1, Integer::sum);

        // escolhe, entre as intenções válidas, a de destino mais "frio" (menos visitado)
        String melhor = validas.get(0);
        int menorVisitas = Integer.MAX_VALUE;
        for (String acao : validas) {
            int[] d = destino(acao, x, y);
            // coordenada ainda não visitada conta como 0
            int visitas = historicoVisitas.getOrDefault(chave(d[0], d[1]), 0);
            if (visitas < menorVisitas) { // '<' estrito mantém a 1ª de contagem mínima (desempate determinístico)
                menorVisitas = visitas;
                melhor = acao;
            }
        }
        return melhor;
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

    // expõe o mapa de calor apenas para leitura (o painel desenha, não altera)
        public Map<String, Integer> getHistoricoVisitas() {
            return Collections.unmodifiableMap(historicoVisitas);
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

