package org.example;
import org.example.rede.ArenaClient;
import org.example.heuristica.MotorHeuristico;
import org.example.modelo.Percecao;
import org.example.rag.MotorRAG;

// Loop Sense-Think-Act 100% autónomo (sem input humano). Só delega.
public class AgenteExplorador {
    private final ArenaClient arena = new ArenaClient(Configuracao.URL_BASE);
    private final MotorHeuristico cerebro = new MotorHeuristico();
    private MotorRAG rag; // injetado quando Kaiky tiver a implementação

    public static void main(String[] args) throws Exception { new AgenteExplorador().correr(); }

    public void correr() throws Exception {
        arena.registar(Configuracao.ROOM_ID, Configuracao.ROBOT_ID);
        // String manual = arena.descarregarManual(Configuracao.ROOM_ID);
        // if (rag != null) rag.ingerirManual(manual);

        while (true) {
            try {
                Percecao p = arena.perceber(Configuracao.ROOM_ID, Configuracao.ROBOT_ID); // SENSE
                if (p.isGame_over()) break;
                if (!p.isGame_started()) { pausar(); continue; } // lobby

                String acao = cerebro.decidirAcao(p);                                     // THINK
                arena.agir(Configuracao.ROOM_ID, Configuracao.ROBOT_ID, acao);            // ACT
            } catch (Exception e) {
                // Resiliência (Secção 9): timeouts/micro-cortes não derrubam o agente.
            }
            pausar(); // anti-flood obrigatório
        }
    }

    private void pausar() {
        try { Thread.sleep(Configuracao.PAUSA_CICLO_MS); } catch (InterruptedException ignored) {}
    }
}