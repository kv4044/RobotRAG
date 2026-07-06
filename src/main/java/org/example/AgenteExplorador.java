package org.example;

import org.example.heuristica.MotorHeuristico;
import org.example.modelo.Percecao;
import org.example.modelo.RespostaRegisto;
import org.example.modelo.Cofre;
import org.example.rag.MotorRAG;
import org.example.rag.MotorRAGImpl;
import org.example.rag.RespostaRAG;
import org.example.rede.ArenaClient;
import org.example.rede.OllamaClient;
import org.example.ui.*;

import javax.swing.JFrame;

// Loop Sense-Think-Act 100% autónomo. Só orquestra e delega.
public class AgenteExplorador {

    private final MotorRAG motorRAG = new MotorRAGImpl(new OllamaClient());

    private boolean manualIngerido = false;

    private final Configuracao config;
    private final ArenaClient arena;
    private MotorHeuristico cerebro;

    // construtor recebe a config vinda do menu


    public static void main(String[] args) throws Exception {
        MenuConfiguracao menu = new MenuConfiguracao();
        if (!menu.mostrar()) {
            System.out.println("Configuração cancelada. A sair.");
            return;
        }
        Configuracao config = new Configuracao(
                menu.getRoomId(), menu.getRobotId(), menu.isModoBatalha());
        new AgenteExplorador(config).correr();
    }

    public AgenteExplorador(Configuracao config) {
        this.config = config;
        this.arena = new ArenaClient(Configuracao.URL_BASE); // URL_BASE continua estático
    }

    public void correr() throws Exception {
        cerebro = new MotorHeuristico(config.isModoBatalha());

        RespostaRegisto reg = arena.registar(config.getRoomId(), config.getRobotId());
        System.out.println("Registado em (" + reg.getEstado().getX() + ","
                + reg.getEstado().getY() + ") energia=" + reg.getEstado().getEnergia());

        motorRAG.ingerirManual(arena.descarregarManual(config.getRoomId()));
        System.out.println("Manual ingerido: " + config.getRoomId());

        PainelMapaCalor painel = new PainelMapaCalor(
                cerebro.getHistoricoVisitas(),
                cerebro.getMurosConhecidos(),
                cerebro.getRecursosConhecidos(),
                cerebro.getCofresConhecidos(),   // <- conhecidos primeiro (bate com o construtor)
                cerebro.getCofresFalhados(),     // <- falhados depois
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
                Percecao p = arena.perceber(config.getRoomId(), config.getRobotId());

                if (p.isGame_over()) {
                    System.out.println("Jogo terminado. A desligar motores.");
                    break;
                }
                if (!p.isGame_started()) {
                    // Lobby: aproveita a espera pelo sinal do professor para ingerir o manual
                    // FORA do cronómetro de 10 min (7.1: relógio só arranca no sinal de início).
                    if (!manualIngerido) {
                        try {
                            motorRAG.ingerirManual(arena.descarregarManual(config.getRoomId()));
                            manualIngerido = true; // só marca APÓS sucesso -> falha re-tenta no próximo turno
                            System.out.println("Manual ingerido no lobby (" + config.getRoomId() + ").");
                        } catch (Exception e) {
                            System.out.println("Ingestão falhou, re-tenta próximo turno: " + e.getMessage());
                        }
                    }
                    pausar();
                    continue;
                }

                // --- Deteção de cofre e tentativa real de desbloqueio (pipeline RAG) ---
                Cofre cofreActual = cerebro.cofreSobActual(p);
                if (cofreActual != null && motorRAG.estaPronto()) {
                    RespostaRAG r = motorRAG.resolverEnigma(cofreActual.getTerminal_desafio());

                    if (r.deveSubmeter()) {
                        String st = arena.desbloquear(
                                config.getRoomId(), config.getRobotId(),
                                r.getChaveFinal(), r.getChunkFinal(), r.getRespostaBrutaLLMFinal());

                        switch (st) {
                            case "sucesso":
                                // báu desapareceu (+100HP) -> remove de toda a memória do cérebro
                                cerebro.registarCofreResolvido(cofreActual.getX(), cofreActual.getY());
                                break;
                            case "falha":
                                cerebro.registarCofreFalhado(cofreActual.getX(), cofreActual.getY());
                                break;
                            case "bloqueado":
                                pausar(); pausar(); // anti-flood: NÃO blacklist, reenvia próximo turno
                                break;
                            case "erro":
                                System.out.println("Unlock dessincronizado (erro).");
                                break;
                            default:
                                System.out.println("Unlock status inesperado: " + st);
                        }
                    } else {
                        // RAG sem chave fiável -> blacklist para não queimar -10HP a adivinhar
                        cerebro.registarCofreFalhado(cofreActual.getX(), cofreActual.getY());
                    }
                }

                // THINK
                String acao = cerebro.decidirAcao(p);

                // ACT
                if (acao != null) {
                    arena.agir(config.getRoomId(), config.getRobotId(), acao);
                    System.out.println("Pos=(" + p.getO_meu_estado().getX() + ","
                            + p.getO_meu_estado().getY() + ") HP=" + p.getO_meu_estado().getEnergia()
                            + " -> " + acao);
                }

                painel.atualizar(
                        p.getO_meu_estado().getX(),
                        p.getO_meu_estado().getY(),
                        p.getOutros_robots()
                );
            } catch (Exception e) {
                System.out.println("Falha no turno (a retomar): " + e.getMessage());
            }
            pausar(); // anti-flood obrigatório
        }
    }

    private void pausar() {
        try {
            Thread.sleep(Configuracao.PAUSA_CICLO_MS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}