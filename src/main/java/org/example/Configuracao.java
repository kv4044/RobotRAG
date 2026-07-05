package org.example;

// Constantes do agente. Tudo fixo no código → autonomia total, nada manual.
public final class Configuracao {
    private Configuracao() {}

    public static final String URL_BASE  = "https://arena.pmonteiro.ovh";
    public static final String ROOM_ID = "25639D"; // id da sala gerada (muda por sessão)
    public static final String ROBOT_ID  = "kk";

    // modo de jogo: true = Batalha (combate on), false = Missão (combate off)
    // editar manualmente conforme a sala de teste até o menu existir
    public static final boolean MODO_BATALHA = false;

    // Física (Secção 7 — confirmada no enunciado).
    public static final int HP_INICIAL = 200;
    public static final int HP_TETO    = 250;
    public static final double RAIO_RADAR = 4.5;

    // Anti-flood (Secção 9): margem ≥ 350-400 ms.
    public static final long PAUSA_CICLO_MS = 400;

}