package org.example.heuristica;

import org.example.modelo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;

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

    // combate só ativa em Batalha (o modo vem do menu de config, não da API)
    private final boolean modoBatalha;

    public MotorHeuristico(boolean modoBatalha) {
        this.modoBatalha = modoBatalha;
    }

        // As quatro intenções base. Confirmadas no Swagger.
    private static final String[] INTENCOES = {
            "MOVER_NORTE", "MOVER_SUL", "MOVER_ESTE", "MOVER_OESTE"
    };

        // memória do cérebro (estado persistente entre turnos)
        private final Map<String, Integer> historicoVisitas = new HashMap<>();
        private final Set<String> murosConhecidos = new HashSet<>();
        private final Set<String> recursosConhecidos = new LinkedHashSet<>();
        private final Set<String> cofresFalhados = new HashSet<>();
        private final Set<String> cofresConhecidos = new HashSet<>();
        private final Set<String> celulasVistas = new HashSet<>();

        // expõem a memória do motor APENAS para leitura (o painel desenha, não altera) — SRP
        public Map<String, Integer> getHistoricoVisitas() { return Collections.unmodifiableMap(historicoVisitas); }
        public Set<String> getMurosConhecidos()  { return Collections.unmodifiableSet(murosConhecidos); }
        public Set<String> getCofresFalhados()   { return Collections.unmodifiableSet(cofresFalhados); }
        public Set<String> getRecursosConhecidos(){ return Collections.unmodifiableSet(recursosConhecidos); }
        public Set<String> getCofresConhecidos() { return Collections.unmodifiableSet(cofresConhecidos); }
        public Set<String> getCelulasVistas() { return Collections.unmodifiableSet(celulasVistas); }

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

            // varre o raio de visão e memoriza como "chão visto" as casas com linha de visão limpa
            varrerCampoVisao(x, y);

            // memoriza recursos visíveis não coletados (para regresso com HP baixo)
            if (p.getRecursos_no_mundo() != null) {
                for (Recurso rec : p.getRecursos_no_mundo()) {
                    if (!rec.isColetado()) {
                        recursosConhecidos.add(chave(rec.getX(), rec.getY()));
                    }
                }
            }

            // remove da memória o recurso onde o robô está agora (coletou-o, ou já não existe)
            recursosConhecidos.remove(chave(x, y));

            // memoriza muros visíveis (limites + obstáculos internos) para filtragem persistente
            if (p.getObjetos_fixos() != null) {
                for (ObjetoFixo m : p.getObjetos_fixos()) {
                    murosConhecidos.add(chave(m.getX(), m.getY()));
                }
            }

            // memoriza cofres visíveis para os mostrar no mapa mesmo antes de os pisar
            if (p.getCofres_no_mundo() != null) {
                for (Cofre c : p.getCofres_no_mundo()) {
                    cofresConhecidos.add(chave(c.getX(), c.getY()));
                }
            }

            // mapa de calor: regista a passagem pela casa atual (casa andada = valor >=1; não andada = 0)
            historicoVisitas.merge(chave(x, y), 1, Integer::sum);

            List<String> validas = filtrarColisoes(p);
            if (validas.isEmpty()) return null;

            // combate tem prioridade máxima em Batalha (abate ou fuga)
            String combate = passoCombate(p, x, y, hp, validas);
            if (combate != null) return combate;

            if (validas.isEmpty()) return null; // cercado por muros: caller não deve mover

            // 2+3) tenta cada alvo por ordem de proximidade; usa BFS que contorna muros.
            //      Se o mais próximo for inalcançável (atrás de parede), passa ao seguinte (opção A).
            List<int[]> alvos = alvosOrdenados(p, x, y, hp);
            for (int[] alvo : alvos) {
                String acao = passoParaAlvoBFS(x, y, alvo, validas);
                if (acao != null) return acao; // rota real encontrada (já contorna a parede)
            }

            // antes: return passoMaisFrio(validas, x, y);
            String rumo = passoExploracao(x, y, validas);
            return (rumo != null) ? rumo : passoMaisFrio(validas, x, y);
        }

        // alvos por prioridade de HP:
        // < 80  -> emergência: recursos + cofres (ambos curam), tudo o que for alcançável
        // < 200 -> oportunista: só recursos SE visíveis/memorizados; cofres continuam a atrair
        // = 250 (cheio) -> só cofres (missão)
        private List<int[]> alvosOrdenados(Percecao p, int x, int y, int hp) {
            List<int[]> candidatos = new ArrayList<>();

            if (hp < 80) {
                // emergência de energia: recursos primeiro (curam rápido), depois cofres
                candidatos.addAll(alvosRecursos(p));
                candidatos.addAll(cofresVisiveis(p));
            } else if (hp < 200) {
                // oportunista: apanha HP no caminho se houver, mas cofres mantêm-se como objetivo
                candidatos.addAll(alvosRecursos(p));
                candidatos.addAll(cofresVisiveis(p));
            } else {
                // HP no teto: foco em cofres (missão)
                candidatos.addAll(cofresVisiveis(p));
            }

            candidatos.sort((a, b) ->
                    Integer.compare(manhattan(x, y, a[0], a[1]), manhattan(x, y, b[0], b[1])));
            return candidatos;
        }

        // candidatos de recurso = memorizados + visíveis agora (união, sem duplicados)
        private List<int[]> alvosRecursos(Percecao p) {
            Set<String> candidatos = new LinkedHashSet<>(recursosConhecidos);
            if (p.getRecursos_no_mundo() != null) {
                for (Recurso rec : p.getRecursos_no_mundo()) {
                    if (!rec.isColetado()) candidatos.add(chave(rec.getX(), rec.getY()));
                }
            }
            List<int[]> alvos = new ArrayList<>();
            for (String c : candidatos) {
                int[] xy = parseChaveInt(c);
                alvos.add(xy);
            }
            return alvos;
        }

        // "x,y" -> int[]{x,y}
        private int[] parseChaveInt(String c) {
            String[] p = c.split(",");
            return new int[]{ Integer.parseInt(p[0]), Integer.parseInt(p[1]) };
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

        // BFS: encontra a casa desconhecida alcançável mais próxima e devolve o 1º passo até lá.
        // Atravessa casas vistas/caminháveis (não-muro); para na 1ª casa fora de celulasVistas.
        // null se não há fronteira alcançável (mapa explorado ou cercado) -> cai no passoMaisFrio.
        private String passoExploracao(int roboX, int roboY, List<String> validas) {
            java.util.Deque<int[]> fila = new java.util.ArrayDeque<>();
            Set<String> visitadosBFS = new HashSet<>();
            // guarda, por cada casa alcançada, a PRIMEIRA ação que iniciou esse ramo
            Map<String, String> primeiraAcao = new HashMap<>();

            // arranca o BFS pelos vizinhos válidos imediatos (respeitam colisão do turno)
            for (String acao : validas) {
                int[] d = destino(acao, roboX, roboY);
                String k = chave(d[0], d[1]);
                if (murosConhecidos.contains(k)) continue;
                if (visitadosBFS.add(k)) {
                    fila.add(d);
                    primeiraAcao.put(k, acao);
                }
            }

            while (!fila.isEmpty()) {
                int[] atual = fila.poll();
                String kAtual = chave(atual[0], atual[1]);

                // fronteira: casa que ainda não foi vista -> destino de exploração
                if (!celulasVistas.contains(kAtual)) {
                    return primeiraAcao.get(kAtual);
                }

                // expande para os 4 vizinhos caminháveis
                for (String acao : INTENCOES) {
                    int[] viz = destino(acao, atual[0], atual[1]);
                    if (viz[0] < 0 || viz[1] < 0) continue;
                    String kViz = chave(viz[0], viz[1]);
                    if (murosConhecidos.contains(kViz)) continue;
                    if (visitadosBFS.add(kViz)) {
                        fila.add(viz);
                        primeiraAcao.put(kViz, primeiraAcao.get(kAtual)); // propaga a ação inicial do ramo
                    }
                }
            }
            return null; // sem fronteira alcançável
        }

        // BFS até uma coordenada-alvo, contornando muros conhecidos. Devolve o 1º passo
        // da rota mais curta, ou null se o alvo for inalcançável (cercado por muros).
        // Custo uniforme (1 HP/passo) -> BFS dá o caminho mínimo sem A*.
        private String passoParaAlvoBFS(int roboX, int roboY, int[] alvo, List<String> validas) {
            String alvoK = chave(alvo[0], alvo[1]);
            java.util.Deque<int[]> fila = new java.util.ArrayDeque<>();
            Set<String> visitadosBFS = new HashSet<>();
            Map<String, String> primeiraAcao = new HashMap<>();

            // arranca pelos vizinhos válidos imediatos (respeitam a colisão do turno)
            for (String acao : validas) {
                int[] d = destino(acao, roboX, roboY);
                String k = chave(d[0], d[1]);
                if (murosConhecidos.contains(k)) continue;
                if (visitadosBFS.add(k)) {
                    fila.add(d);
                    primeiraAcao.put(k, acao);
                }
            }

            while (!fila.isEmpty()) {
                int[] atual = fila.poll();
                String kAtual = chave(atual[0], atual[1]);

                if (kAtual.equals(alvoK)) return primeiraAcao.get(kAtual); // chegou ao alvo

                for (String acao : INTENCOES) {
                    int[] viz = destino(acao, atual[0], atual[1]);
                    if (viz[0] < 0 || viz[1] < 0) continue;
                    String kViz = chave(viz[0], viz[1]);
                    if (murosConhecidos.contains(kViz)) continue;   // nunca atravessa muro
                    if (visitadosBFS.add(kViz)) {
                        fila.add(viz);
                        primeiraAcao.put(kViz, primeiraAcao.get(kAtual)); // propaga ação inicial do ramo
                    }
                }
            }
            return null; // alvo inalcançável com o que se conhece do mapa
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

        // entre as ações válidas, a que mais aproxima do alvo, nunca entrando em muro conhecido.
        // Desempate pela casa mais fria. Devolve null se nenhum passo aproxima -> fallback heatmap.
        private String passoParaAlvo(List<String> validas, int x, int y, int[] alvo) {
            int distAtual = manhattan(x, y, alvo[0], alvo[1]);
            String melhor = null;
            int melhorDist = Integer.MAX_VALUE;
            int melhorVisitas = Integer.MAX_VALUE;
            for (String acao : validas) {
                int[] d = destino(acao, x, y);
                if (murosConhecidos.contains(chave(d[0], d[1]))) continue; // nunca ir para muro conhecido
                int dist = manhattan(d[0], d[1], alvo[0], alvo[1]);
                if (dist >= distAtual) continue; // só passos que aproximam
                int visitas = historicoVisitas.getOrDefault(chave(d[0], d[1]), 0);
                if (dist < melhorDist || (dist == melhorDist && visitas < melhorVisitas)) {
                    melhorDist = dist; melhorVisitas = visitas; melhor = acao;
                }
            }
            return melhor;
        }

        // bloco adjacente mais frio (comportamento base de exploração)
        private String passoMaisFrio(List<String> validas, int x, int y) {
            String melhor = null;
            int menorVisitas = Integer.MAX_VALUE;
            for (String acao : validas) {
                int[] d = destino(acao, x, y);
                if (murosConhecidos.contains(chave(d[0], d[1]))) continue; // evita muro memorizado
                int visitas = historicoVisitas.getOrDefault(chave(d[0], d[1]), 0);
                if (visitas < menorVisitas) { menorVisitas = visitas; melhor = acao; }
            }
            // se todas as válidas caírem em muro conhecido (raro), usa a 1ª válida como último recurso
            return (melhor != null) ? melhor : validas.get(0);
        }

        // marca em celulasVistas todas as casas dentro do raio 4.5 cuja linha de visão
        // até ao robô não é cortada por um muro conhecido (oclusão, §7).
        private void varrerCampoVisao(int roboX, int roboY) {
            int raio = 4; // 4.5 euclidiano: dx,dy até 4 (5º já excede em qualquer eixo)
            for (int dx = -raio; dx <= raio; dx++) {
                for (int dy = -raio; dy <= raio; dy++) {
                    if (dx * dx + dy * dy > 4.5 * 4.5) continue; // fora do círculo de raio 4.5
                    int cx = roboX + dx;
                    int cy = roboY + dy;
                    if (cx < 0 || cy < 0) continue;              // arena só tem coords positivas
                    if (temLinhaDeVisao(roboX, roboY, cx, cy)) {
                        celulasVistas.add(chave(cx, cy));         // inclui a própria casa do muro (é vista)
                    }
                }
            }
        }

        // linha de visão limpa: nenhum muro conhecido ENTRE o robô e o alvo.
        // A casa-alvo pode ser muro (vê-se a parede); só bloqueiam muros no caminho, não o destino.
        private boolean temLinhaDeVisao(int x0, int y0, int x1, int y1) {
            // amostragem por passos ao longo da reta (DDA simples)
            int passos = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
            if (passos == 0) return true; // a própria casa do robô
            double dx = (x1 - x0) / (double) passos;
            double dy = (y1 - y0) / (double) passos;
            // percorre pontos intermédios (exclui origem; exclui destino para não auto-bloquear muros-alvo)
            for (int i = 1; i < passos; i++) {
                int px = (int) Math.round(x0 + dx * i);
                int py = (int) Math.round(y0 + dy * i);
                if (murosConhecidos.contains(chave(px, py))) return false; // muro corta a visão
            }
            return true;
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

        // rival mais próximo VISÍVEL (dentro do raio de radar). Usado para perseguição sustentada.
        // Devolve null fora de Batalha ou sem rivais.
        private Map.Entry<String, OutroRobot> rivalVisivel(Percecao p, int x, int y) {
            if (!modoBatalha || p.getOutros_robots() == null) return null;
            java.util.Map.Entry<String, OutroRobot> maisPerto = null;
            int menor = Integer.MAX_VALUE;
            for (java.util.Map.Entry<String, OutroRobot> e : p.getOutros_robots().entrySet()) {
                OutroRobot r = e.getValue();
                int d = manhattan(x, y, r.getX(), r.getY());
                if (d < menor) { menor = d; maisPerto = e; }
            }
            return maisPerto; // o servidor só devolve rivais dentro do radar -> já estão visíveis
        }

        // decisão de combate (§10). Prioridade máxima em Batalha.
        // - rival com MENOS HP que o nosso e visível -> perseguir/atacar até ao abate (nunca foge).
        // - rival com HP >= o nosso e a <= 2 blocos -> fugir (Nível 2, BFS).
        // - caso contrário -> null (segue a missão).
        private String passoCombate(Percecao p, int x, int y, int hp, List<String> validas) {
            java.util.Map.Entry<String, OutroRobot> alvo = rivalVisivel(p, x, y);
            if (alvo == null) return null;

            OutroRobot rival = alvo.getValue();
            int hpRival = rival.getEnergia();
            int dist = manhattan(x, y, rival.getX(), rival.getY());

            if (hp > hpRival) {
                // ABATE: enquanto visível e mais fraco, persegue com BFS (contorna muros).
                // A investida é o próprio movimento para a casa do rival.
                String passo = passoParaAlvoBFS(x, y, new int[]{rival.getX(), rival.getY()}, validas);
                if (passo != null) return passo;
                // rival visível mas inalcançável agora (muro entre ambos) -> não desiste, aproxima-se pelo mapa
                return passoExploracao(x, y, validas);
            }

            // rival mais forte ou igual: só foge se estiver perto o suficiente para ser ameaça
            if (dist <= 2) {
                return passoFugaBFS(x, y, rival, validas);
            }
            return null; // rival forte mas longe: ignora, segue missão
        }

        // Fuga Nível 2: BFS a partir da posição atual, dentro de um horizonte de passos,
        // escolhe a casa alcançável (contornando muros) que MAXIMIZA a distância ao rival
        // e devolve o 1º passo dessa rota. Evita becos porque só considera casas com caminho real.
        private String passoFugaBFS(int roboX, int roboY, OutroRobot rival, List<String> validas) {
            final int HORIZONTE = 4; // igual ao raio de visão: foge até ao limite do que conhece

            java.util.Deque<int[]> fila = new java.util.ArrayDeque<>(); // {x, y, profundidade}
            Set<String> visitadosBFS = new HashSet<>();
            Map<String, String> primeiraAcao = new HashMap<>();

            String melhorAcao = null;
            int melhorDistRival = -1;

            for (String acao : validas) {
                int[] d = destino(acao, roboX, roboY);
                String k = chave(d[0], d[1]);
                if (murosConhecidos.contains(k)) continue;
                if (visitadosBFS.add(k)) {
                    fila.add(new int[]{d[0], d[1], 1});
                    primeiraAcao.put(k, acao);
                }
            }

            while (!fila.isEmpty()) {
                int[] atual = fila.poll();
                String kAtual = chave(atual[0], atual[1]);
                int prof = atual[2];

                // avalia esta casa como candidato de fuga (mais longe do rival = melhor)
                int distRival = manhattan(atual[0], atual[1], rival.getX(), rival.getY());
                if (distRival > melhorDistRival) {
                    melhorDistRival = distRival;
                    melhorAcao = primeiraAcao.get(kAtual);
                }

                if (prof >= HORIZONTE) continue; // não expande além do horizonte

                for (String acao : INTENCOES) {
                    int[] viz = destino(acao, atual[0], atual[1]);
                    if (viz[0] < 0 || viz[1] < 0) continue;
                    String kViz = chave(viz[0], viz[1]);
                    if (murosConhecidos.contains(kViz)) continue;
                    if (visitadosBFS.add(kViz)) {
                        fila.add(new int[]{viz[0], viz[1], prof + 1});
                        primeiraAcao.put(kViz, primeiraAcao.get(kAtual));
                    }
                }
            }
            return melhorAcao; // null só se cercado; o decidirAcao trata o fallback
        }
}

