package org.example;

import org.example.heuristica.MotorHeuristico;
import org.example.modelo.Percecao;
import org.example.modelo.RespostaRegisto;
import org.example.modelo.Cofre;
import org.example.rede.ArenaClient;
import org.example.ui.PainelMapaCalor;

import javax.swing.JFrame;

// Loop Sense-Think-Act 100% autónomo. Só orquestra e delega.
public class AgenteExplorador {

    private final ArenaClient arena = new ArenaClient(Configuracao.URL_BASE);
    private final MotorHeuristico cerebro = new MotorHeuristico();

    // PLACEHOLDER TEMPORÁRIO — substituir pela integração RAG+/unlock do Kaiky.
    // Simula sempre {"status":"falha"} para o robô marcar o cofre e sair, evitando o loop.
    private String tentarUnlockPlaceholder(Cofre cofre) {
        System.out.println("[PLACEHOLDER] Cofre em (" + cofre.getX() + "," + cofre.getY()
                + ") detetado. Enigma: " + cofre.getTerminal_desafio());
        System.out.println("[PLACEHOLDER] A simular falha de unlock (RAG do Kaiky ainda nao ligado).");
        return "falha";
    }

    public static void main(String[] args) throws Exception {
        new AgenteExplorador().correr();
    }

    public void correr() throws Exception {
        RespostaRegisto reg = arena.registar(Configuracao.ROOM_ID, Configuracao.ROBOT_ID);
        System.out.println("Registado em (" + reg.getEstado().getX() + ","
                + reg.getEstado().getY() + ") energia=" + reg.getEstado().getEnergia());

        PainelMapaCalor painel = new PainelMapaCalor(
                cerebro.getHistoricoVisitas(),
                cerebro.getMurosConhecidos(),
                cerebro.getRecursosConhecidos(),
                cerebro.getCofresFalhados(),
                cerebro.getCofresConhecidos(),
                cerebro.getCelulasVistas()
        );

        JFrame janela = new JFrame("Mapa de Calor - NeymarRAG");
        janela.add(painel);
        janela.setSize(600, 600);
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setVisible(true);

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

                // --- Deteção de cofre e (placeholder) tentativa de desbloqueio ---
                Cofre cofreActual = cerebro.cofreSobActual(p);
                if (cofreActual != null) {
                    // FRONTEIRA KAIKY: aqui entrará a chamada real ao /unlock, com a chave
                    // gerada pelo pipeline RAG a partir de cofreActual.getTerminal_desafio().
                    // Por agora, placeholder que simula sempre falha para quebrar o loop.
                    String status = tentarUnlockPlaceholder(cofreActual);

                    if ("falha".equals(status)) {
                        // marca o cofre para deixar de o atrair -> quebra o loop entra/sai
                        cerebro.registarCofreFalhado(cofreActual.getX(), cofreActual.getY());
                    }
                    // (quando houver "sucesso" real: opcionalmente injetar fuga na filaAcoesPlaneadas)
                }

                // THINK - decisão de movimento normal (atração já ignora cofres falhados)
                String acao = cerebro.decidirAcao(p);

                // ACT
                if (acao != null) {
                    arena.agir(Configuracao.ROOM_ID, Configuracao.ROBOT_ID, acao);
                    System.out.println("Pos=(" + p.getO_meu_estado().getX() + ","
                            + p.getO_meu_estado().getY() + ") HP=" + p.getO_meu_estado().getEnergia()
                            + " -> " + acao);

                }

                // no fim de cada ciclo Sense-Think-Act:
                painel.atualizar(
                        p.getO_meu_estado().getX(),
                        p.getO_meu_estado().getY(),
                        p.getOutros_robots()
                );
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