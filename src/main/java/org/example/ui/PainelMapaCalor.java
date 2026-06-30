package org.example.ui;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.util.Map;
import org.example.modelo.Percecao;

// SÓ Swing/Graphics2D. Não faz rede nem decisão.
public class PainelMapaCalor extends JPanel {
    @Override protected void paintComponent(Graphics g) { super.paintComponent(g); }
    public void atualizar(Percecao p, Map<String,Integer> historicoVisitas) {}
}