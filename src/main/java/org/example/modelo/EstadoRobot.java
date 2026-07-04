package org.example.modelo;

// Estado do próprio robô. Mapeia "estado" (/register) e "o_meu_estado" (/perceive).
// Campos confirmados no Swagger: x, y, z, energia, cor.
public class EstadoRobot {

    private int x;
    private int y;
    private double z;
    private int energia;
    private String cor;

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    public int getEnergia() { return energia; }
    public void setEnergia(int energia) { this.energia = energia; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
}