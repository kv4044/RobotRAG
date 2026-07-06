package org.example.ui;

import javax.swing.*;
import java.awt.GridLayout;

// Ecrã inicial mínimo: recolhe robot_id, room_id e modo antes de arrancar o agente.
// Só recolhe input e devolve os valores (SRP: não arranca o jogo, não faz HTTP).
public class MenuConfiguracao {

    private String robotId;
    private String roomId;
    private boolean modoBatalha;

    // mostra o diálogo modal; devolve true se o utilizador confirmou, false se cancelou
    public boolean mostrar() {
        JTextField campoRobot = new JTextField("kk");
        JTextField campoSala = new JTextField("D7EE87");
        String[] modos = {"Missão", "Batalha"};
        JComboBox<String> comboModo = new JComboBox<>(modos);

        JPanel painel = new JPanel(new GridLayout(0, 1, 4, 4));
        painel.add(new JLabel("ID do Robô:"));
        painel.add(campoRobot);
        painel.add(new JLabel("ID da Sala:"));
        painel.add(campoSala);
        painel.add(new JLabel("Modo de Jogo:"));
        painel.add(comboModo);

        int res = JOptionPane.showConfirmDialog(
                null, painel, "Configuração do Agente - NeymarRAG",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (res != JOptionPane.OK_OPTION) return false;

        this.robotId = campoRobot.getText().trim();
        this.roomId = campoSala.getText().trim();
        this.modoBatalha = comboModo.getSelectedIndex() == 1; // 1 = Batalha
        return true;
    }

    public String getRobotId()      { return robotId; }
    public String getRoomId()       { return roomId; }
    public boolean isModoBatalha()  { return modoBatalha; }
}