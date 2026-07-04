package org.example;

import org.example.heuristica.MotorHeuristico;
import org.example.modelo.Percecao;
import org.example.modelo.RespostaRegisto;
import org.example.rede.ArenaClient;

// Loop Sense-Think-Act 100% autónomo. Só orquestra e delega.
public class AgenteExplorador {

    private final ArenaClient arena = new ArenaClient(Configuracao.URL_BASE);
    private final MotorHeuristico cerebro = new MotorHeuristico();

    public static void main(String[] args) throws Exception {
        new AgenteExplorador().correr();
    }

    public void correr() throws Exception {
        RespostaRegisto reg = arena.registar(Configuracao.ROOM_ID, Configuracao.ROBOT_ID);
        System.out.println("Registado em (" + reg.getEstado().getX() + ","
                + reg.getEstado().getY() + ") energia=" + reg.getEstado().getEnergia());

        while (true) {
            try {
                // SENSE
                Percecao p = arena.perceber(Configuracao.ROOM_ID, Configuracao.ROBOT_ID);

                if (p.isGame_over()) {
                    System.out.println("Jogo terminado. A desligar motores.");
                    break;
                }
                if (!p.isGame_started()) {
                    // Lobby: aguarda o início sem consumir a decisão.
                    pausar();
                    continue;
                }

                // THINK
                String acao = cerebro.decidirAcao(p);

                // ACT
                arena.agir(Configuracao.ROOM_ID, Configuracao.ROBOT_ID, acao);
                System.out.println("Pos=(" + p.getO_meu_estado().getX() + ","
                        + p.getO_meu_estado().getY() + ") HP=" + p.getO_meu_estado().getEnergia()
                        + " -> " + acao);

            } catch (Exception e) {
                // Resiliência: timeouts/micro-cortes não derrubam o agente.
                System.out.println("Falha no turno (a retomar): " + e.getMessage());
            }
            pausar(); // anti-flood obrigatório
        }
    }

    // Pausa entre turnos. Margem ≥ 350-400ms para o Jitter da rede.
    private void pausar() {
        try {
            Thread.sleep(Configuracao.PAUSA_CICLO_MS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}