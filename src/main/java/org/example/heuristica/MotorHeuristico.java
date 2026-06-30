package org.example.heuristica;
import java.util.*;
import org.example.modelo.Percecao;

// Cérebro determinístico. Decide a ação SOZINHO. Não faz HTTP/UI/Ollama.
public class MotorHeuristico {
    private final Map<String,Integer> historicoVisitas = new HashMap<>(); // mapa de calor
    private final Set<String> cofresFalhados = new HashSet<>();           // lista negra
    private final Deque<String> filaAcoesPlaneadas = new ArrayDeque<>();  // reflexos

    // Pipeline: fila planeada → filtrar colisões → atração magnética → bloco mais frio.
    public String decidirAcao(Percecao p) { return null; }

    private List<String> filtrarColisoes(Percecao p) { return null; }
    private String escolherBlocoMaisFrio(Percecao p, List<String> validas) { return null; }
    private String rotaParaAlvoMaisProximo(Percecao p) { return null; }

    public void registarVisita(int x, int y) {}
    public void marcarCofreFalhado(int x, int y) {}
    public void injetarFuga(/* direção oposta ao inimigo */) {}
    public boolean temAcoesPlaneadas() { return !filaAcoesPlaneadas.isEmpty(); }
    public String proximaAcaoPlaneada() { return filaAcoesPlaneadas.poll(); }
}