package org.example.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;
import org.example.modelo.Recurso;
import org.example.modelo.Cofre;

// Só desenha (SRP). Recebe o mapa de calor (leitura), a posição do robô,
// e as listas de recursos/cofres visíveis do turno.
public class PainelMapaCalor extends JPanel {

    private final Map<String, Integer> historicoVisitas;
    private int xRobo = 0;
    private int yRobo = 0;
    private List<Recurso> recursos;
    private List<Cofre> cofres;

    // cor única para casas andadas (o "calor" agora é o número, não a cor)
    private static final Color COR_ANDADA = new Color(60, 90, 160);

    public PainelMapaCalor(Map<String, Integer> historicoVisitas) {
        this.historicoVisitas = historicoVisitas;
        setBackground(Color.BLACK);
    }

    // chamado pelo AgenteExplorador no fim de cada ciclo
    public void atualizar(int x, int y, List<Recurso> recursos, List<Cofre> cofres) {
        this.xRobo = x;
        this.yRobo = y;
        this.recursos = recursos;
        this.cofres = cofres;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // 1) dimensões dinâmicas da grelha
        int maxX = xRobo;
        int maxY = yRobo;
        for (String k : historicoVisitas.keySet()) {
            int[] c = parseChave(k);
            if (c[0] > maxX) maxX = c[0];
            if (c[1] > maxY) maxY = c[1];
        }
        // inclui recursos/cofres no cálculo para caberem sempre
        if (recursos != null) for (Recurso r : recursos) { if (r.getX() > maxX) maxX = r.getX(); if (r.getY() > maxY) maxY = r.getY(); }
        if (cofres != null)   for (Cofre c : cofres)     { if (c.getX() > maxX) maxX = c.getX(); if (c.getY() > maxY) maxY = c.getY(); }

        int colunas = maxX + 1;
        int linhas = maxY + 1;
        int lado = Math.min(getWidth() / colunas, getHeight() / linhas);
        if (lado < 1) lado = 1;

        // 2) casas andadas: cor única + número da contagem no centro
        g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(8, lado / 3)));
        for (Map.Entry<String, Integer> e : historicoVisitas.entrySet()) {
            int[] c = parseChave(e.getKey());
            int px = c[0] * lado;
            int py = (linhas - 1 - c[1]) * lado; // Y invertido
            g2.setColor(COR_ANDADA);
            g2.fillRect(px, py, lado, lado);
            // número da contagem centrado
            g2.setColor(Color.WHITE);
            String txt = String.valueOf(e.getValue());
            int tw = g2.getFontMetrics().stringWidth(txt);
            int th = g2.getFontMetrics().getAscent();
            g2.drawString(txt, px + (lado - tw) / 2, py + (lado + th) / 2);
        }

        // 3) recursos (verde) por cima da grelha
        if (recursos != null) {
            g2.setColor(Color.GREEN);
            for (Recurso r : recursos) {
                if (r.isColetado()) continue;
                int px = r.getX() * lado;
                int py = (linhas - 1 - r.getY()) * lado;
                g2.fillRect(px, py, lado, lado);
            }
        }

        // 4) cofres (amarelo)
        if (cofres != null) {
            g2.setColor(Color.YELLOW);
            for (Cofre c : cofres) {
                int px = c.getX() * lado;
                int py = (linhas - 1 - c.getY()) * lado;
                g2.fillRect(px, py, lado, lado);
            }
        }

        // 5) linhas da grelha
        g2.setColor(new Color(40, 40, 40));
        for (int i = 0; i <= colunas; i++) g2.drawLine(i * lado, 0, i * lado, linhas * lado);
        for (int j = 0; j <= linhas; j++) g2.drawLine(0, j * lado, colunas * lado, j * lado);

        // 6) robô (ciano) por cima de tudo
        g2.setColor(Color.CYAN);
        g2.fillOval(xRobo * lado, (linhas - 1 - yRobo) * lado, lado, lado);
    }

    private int[] parseChave(String chave) {
        String[] partes = chave.split(",");
        return new int[]{ Integer.parseInt(partes[0]), Integer.parseInt(partes[1]) };
    }
}