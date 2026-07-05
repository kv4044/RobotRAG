package org.example.modelo;

// Resposta de /arena/action. Confirmado no Swagger: status + nova_posicao.
public class RespostaAcao {
    private String status;              // ex.: "sucesso"; "bloqueado"/"eliminado" ainda por confirmar
    private NovaPosicao nova_posicao;   // presente quando o movimento é válido

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public NovaPosicao getNova_posicao() { return nova_posicao; }
    public void setNova_posicao(NovaPosicao nova_posicao) { this.nova_posicao = nova_posicao; }

    // Classe aninhada: a nova posição só tem x, y, z (sem energia/cor).
    public static class NovaPosicao {
        private int x;
        private int y;
        private double z;
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public double getZ() { return z; }
        public void setZ(double z) { this.z = z; }
    }
}