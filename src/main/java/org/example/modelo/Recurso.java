package org.example.modelo;

// Recurso (esfera de energia) devolvido em recursos_no_mundo
public class Recurso {
    private String id;
    private String type;
    private int x;
    private int y;
    private double z;
    private boolean coletado;

    public String getId() { return id; }
    public String getType() { return type; }
    public int getX() { return x; }
    public int getY() { return y; }
    public double getZ() { return z; }
    public boolean isColetado() { return coletado; }
}