package org.example;

// Constantes do agente. Tudo fixo no código → autonomia total, nada manual.
public final class Configuracao {
    private Configuracao() {}

    public static final String URL_BASE  = "https://arena.pmonteiro.ovh";
    public static final String ROOM_ID = "93A635"; // id da sala gerada (muda por sessão)
    public static final String ROBOT_ID  = "Alfa";

    // Física (Secção 7 — confirmada no enunciado).
    public static final int HP_INICIAL = 200;
    public static final int HP_TETO    = 250;
    public static final double RAIO_RADAR = 4.5;

    // Anti-flood (Secção 9): margem ≥ 350-400 ms.
    public static final long PAUSA_CICLO_MS = 400;

    // Confiança mínima do RAG para autorizar /unlock (evita -10 HP por palpite).
    // Calibrar na sala de treino.
    public static final double LIMIAR_SIMILARIDADE = 0.75;
}