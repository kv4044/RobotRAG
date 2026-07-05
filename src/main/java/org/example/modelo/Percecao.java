package org.example.modelo;

import java.util.List;
import java.util.Map;

public class Percecao {
    private EstadoRobot o_meu_estado;
    private java.util.List<org.example.modelo.Recurso> recursos_no_mundo;
    private List<ObjetoFixo> objetos_fixos;
    private Map<String, OutroRobot> outros_robots;
    private java.util.List<org.example.modelo.Cofre> cofres_no_mundo;
    private boolean game_started;
    private boolean game_over;
    private String vencedor;

    // getters (necessários para o orquestrador ler o estado)
    public EstadoRobot getO_meu_estado() { return o_meu_estado; }
    public java.util.List<org.example.modelo.Recurso> getRecursos_no_mundo() { return recursos_no_mundo; }
    public List<ObjetoFixo> getObjetos_fixos() { return objetos_fixos; }
    public Map<String, OutroRobot> getOutros_robots() { return outros_robots; }
    public java.util.List<org.example.modelo.Cofre> getCofres_no_mundo() { return cofres_no_mundo; }public boolean isGame_started() { return game_started; }
    public boolean isGame_over() { return game_over; }
    public String getVencedor() { return vencedor; }
    
}