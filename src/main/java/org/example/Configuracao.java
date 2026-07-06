package org.example;

// Configuração da sessão. Identidade (sala/robô/modo) vem do menu -> instância imutável.
// Constantes de física/rede permanecem estáticas (não mudam por sessão).
public final class Configuracao {

    // --- estáticas: física e rede (Secção 7/9, não mudam por sessão) ---
    public static final String URL_BASE = "https://arena.pmonteiro.ovh";
    public static final int HP_INICIAL = 200;
    public static final int HP_TETO    = 250;
    public static final double RAIO_RADAR = 4.5;
    public static final long PAUSA_CICLO_MS = 400;

    // --- instância: definida pelo menu no arranque, imutável durante a partida ---
    private final String roomId;
    private final String robotId;
    private final boolean modoBatalha;

    public Configuracao(String roomId, String robotId, boolean modoBatalha) {
        this.roomId = roomId;
        this.robotId = robotId;
        this.modoBatalha = modoBatalha;
    }

    public String getRoomId()      { return roomId; }
    public String getRobotId()     { return robotId; }
    public boolean isModoBatalha() { return modoBatalha; }
}