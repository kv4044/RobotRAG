package org.example.modelo;

// Cofre (Terminal de Plasma) devolvido em cofres_no_mundo.
// terminal_desafio só vem preenchido quando o robô está SOBRE o cofre.
public class Cofre {
    private String id;
    private int x;
    private int y;
    private String terminal_desafio;

    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getTerminal_desafio() { return terminal_desafio; }
}