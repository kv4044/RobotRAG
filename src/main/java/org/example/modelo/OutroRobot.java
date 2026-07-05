package org.example.modelo;

// Robô rival devolvido no mapa outros_robots. Chave do mapa = id; valor = estes campos.
public class OutroRobot {
    private int x;
    private int y;
    private double z;
    private int energia;
    private String cor;

    public int getX() { return x; }
    public int getY() { return y; }
    public double getZ() { return z; }
    public int getEnergia() { return energia; }
    public String getCor() { return cor; }
}