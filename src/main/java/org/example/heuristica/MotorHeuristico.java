package org.example.heuristica;

import org.example.modelo.ObjetoFixo;
import org.example.modelo.Percecao;
import org.example.ui.PainelMapaCalor;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import org.example.modelo.Recurso;
import org.example.modelo.Cofre;

    // Cérebro determinístico. Decide a ação SOZINHO. Não faz HTTP/UI/Ollama.
    public class MotorHeuristico {

    // registar_cofre_falhado: chamado pelo AgenteExplorador quando o /unlock devolve {"status":"falha"}.
    // O motor não faz HTTP; só memoriza a coordenada para deixar de a atrair.
    public void registarCofreFalhado(int x, int y) {
        cofresFalhados.add(chave(x, y));
    }

    // cofreSobActual: devolve o Cofre em cima do qual o robô está (coords iguais e não falhado),
    // ou null. Serve para o orquestrador saber quando acionar o /unlock. Não faz HTTP.
    public Cofre cofreSobActual(Percecao p) {
        int x = p.getO_meu_estado().getX();
        int y = p.getO_meu_estado().getY();
        if (p.getCofres_no_mundo() == null) return null;
        for (Cofre c : p.getCofres_no_mundo()) {
            if (c.getX() == x && c.getY() == y
                    && !cofresFalhados.contains(chave(x, y))) {
                return c;
            }
        }
        return null;
    }


        // As quatro intenções base. Confirmadas no Swagger.
    private static final String[] INTENCOES = {
            "MOVER_NORTE", "MOVER_SUL", "MOVER_ESTE", "MOVER_OESTE"
    };

        // mapa de calor: chave "x,y" -> nº de vezes que o robô pisou essa coordenada
        private final Map<String, Integer> historicoVisitas = new HashMap<>();

        // coordenadas de cofres já falhados; populado na Fase 5 (após confirmar o status do /unlock no Swagger)
        private final Set<String> cofresFalhados = new HashSet<>();

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


        // ajusta o tipo/nome de 'p' ao teu DTO real do /perceive
        public String decidirAcao(Percecao p) {
            int x = p.getO_meu_estado().getX();
            int y = p.getO_meu_estado().getY();
            int hp = p.getO_meu_estado().getEnergia();

            // mapa de calor: regista a passagem pela casa atual (casa andada = valor >=1; não andada = 0)
            historicoVisitas.merge(chave(x, y), 1, Integer::sum);

            // 1) intenções base filtradas por colisões (método já existente)
            List<String> validas = filtrarColisoes(p);
            if (validas.isEmpty()) return null; // cercado por muros: caller não deve mover

            // 2) escolher alvo de atração (recurso ou cofre)
            int[] alvo = escolherAlvo(p, x, y, hp);

            // 3) se há alvo, dar o passo que mais aproxima (Manhattan = nº de passos = HP gasto);
            //    se um muro bloquear a aproximação, cai no mapa de calor para contornar
            if (alvo != null) {
                String acao = passoParaAlvo(validas, x, y, alvo);
                if (acao != null) return acao;
            }
            return passoMaisFrio(validas, x, y);
        }

        // devolve as coords do alvo mais atrativo, ou null se nenhum visível
        private int[] escolherAlvo(Percecao p, int x, int y, int hp) {
            int[] recurso = maisProximo(recursosVisiveis(p), x, y);
            int[] cofre = maisProximo(cofresVisiveis(p), x, y);

            // com HP <= 50, sobrevivência primeiro: prioriza recurso se existir
            if (hp <= 50 && recurso != null) return recurso;

            // caso geral: qualquer alvo visível atrai; escolhe o mais próximo por Manhattan
            if (recurso == null) return cofre;
            if (cofre == null) return recurso;
            int dR = manhattan(x, y, recurso[0], recurso[1]);
            int dC = manhattan(x, y, cofre[0], cofre[1]);
            return (dR <= dC) ? recurso : cofre;
        }

        // recursos ainda não coletados
        private List<int[]> recursosVisiveis(Percecao p) {
            List<int[]> r = new ArrayList<>();
            if (p.getRecursos_no_mundo() == null) return r;
            for (Recurso rec : p.getRecursos_no_mundo()) {
                if (!rec.isColetado()) r.add(new int[]{rec.getX(), rec.getY()});
            }
            return r;
        }

        // cofres que não estão na lista negra
        private List<int[]> cofresVisiveis(Percecao p) {
            List<int[]> r = new ArrayList<>();
            if (p.getCofres_no_mundo() == null) return r;
            for (Cofre c : p.getCofres_no_mundo()) {
                if (!cofresFalhados.contains(chave(c.getX(), c.getY()))) {
                    r.add(new int[]{c.getX(), c.getY()});
                }
            }
            return r;
        }

        // alvo com menor distância de Manhattan; null se lista vazia
        private int[] maisProximo(List<int[]> alvos, int x, int y) {
            int[] melhor = null;
            int menor = Integer.MAX_VALUE;
            for (int[] a : alvos) {
                int d = manhattan(x, y, a[0], a[1]);
                if (d < menor) { menor = d; melhor = a; }
            }
            return melhor;
        }

        private int manhattan(int x1, int y1, int x2, int y2) {
            return Math.abs(x1 - x2) + Math.abs(y1 - y2);
        }

        // entre as ações válidas, a que mais aproxima do alvo; desempate pela casa mais fria
        private String passoParaAlvo(List<String> validas, int x, int y, int[] alvo) {
            int distAtual = manhattan(x, y, alvo[0], alvo[1]);
            String melhor = null;
            int melhorDist = Integer.MAX_VALUE;
            int melhorVisitas = Integer.MAX_VALUE;
            for (String acao : validas) {
                int[] d = destino(acao, x, y);
                int dist = manhattan(d[0], d[1], alvo[0], alvo[1]);
                if (dist >= distAtual) continue; // só passos que APROXIMAM (estrito)
                int visitas = historicoVisitas.getOrDefault(chave(d[0], d[1]), 0);
                if (dist < melhorDist || (dist == melhorDist && visitas < melhorVisitas)) {
                    melhorDist = dist;
                    melhorVisitas = visitas;
                    melhor = acao;
                }
            }
            return melhor; // null se muro impede aproximar -> fallback mapa de calor
        }

        // bloco adjacente mais frio (comportamento base de exploração)
        private String passoMaisFrio(List<String> validas, int x, int y) {
            String melhor = validas.get(0);
            int menorVisitas = Integer.MAX_VALUE;
            for (String acao : validas) {
                int[] d = destino(acao, x, y);
                int visitas = historicoVisitas.getOrDefault(chave(d[0], d[1]), 0);
                if (visitas < menorVisitas) {
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

