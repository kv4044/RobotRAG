package org.example;
import org.example.modelo.RespostaRegisto;
import org.example.rede.ArenaClient;
import org.example.heuristica.MotorHeuristico;
import org.example.modelo.Percecao;
import org.example.rag.MotorRAG;

// Loop Sense-Think-Act 100% autónomo (sem input humano). Só delega.
public class AgenteExplorador {
    private final ArenaClient arena = new ArenaClient(Configuracao.URL_BASE);
    private final MotorHeuristico cerebro = new MotorHeuristico();
    private MotorRAG rag; // injetado quando Kaiky tiver a implementação

    public static void main(String[] args) throws Exception {
        ArenaClient arena = new ArenaClient(Configuracao.URL_BASE);

        RespostaRegisto reg = arena.registar(Configuracao.ROOM_ID, Configuracao.ROBOT_ID);
        System.out.println("Registo: " + reg.getStatus()
                + " em (" + reg.getEstado().getX() + "," + reg.getEstado().getY() + ")"
                + " energia=" + reg.getEstado().getEnergia());

        Percecao p = arena.perceber(Configuracao.ROOM_ID, Configuracao.ROBOT_ID);
        System.out.println("Perceção: pos=(" + p.getO_meu_estado().getX() + ","
                + p.getO_meu_estado().getY() + ") game_started=" + p.isGame_started());
    }
}