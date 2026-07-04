package org.example.modelo;

// Resposta de /arena/{room_id}/register/{robot_id}. Confirmado no Swagger.
public class RespostaRegisto {
    private String status;       // "registado"
    private EstadoRobot estado;  // x, y, z, energia, cor

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public EstadoRobot getEstado() { return estado; }
    public void setEstado(EstadoRobot estado) { this.estado = estado; }
}