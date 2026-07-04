package org.example.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;

// Só desenha: recebe o mapa de calor (leitura) e a posição do robô. Não decide nada (SRP).
public class PainelMapaCalor extends JPanel {

    // referência só-de-leitura ao mapa de calor do MotorHeuristico
    private final Map<String, Integer> historicoVisitas;

    // posição atual do robô (atualizada a cada ciclo pelo AgenteExplorador)
    private int xRobo = 0;
    private int yRobo = 0;

    public PainelMapaCalor(Map<String, Integer> historicoVisitas) {
        this.historicoVisitas = historicoVisitas;
        setBackground(Color.BLACK);
    }

    // chamado pelo AgenteExplorador no fim de cada ciclo Sense-Think-Act
    public void atualizar(int x, int y) {
        this.xRobo = x;
        this.yRobo = y;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // 1) descobrir as dimensões atuais da grelha (auto-ajuste dinâmico)
        int maxX = xRobo;
        int maxY = yRobo;
        int maxVisitas = 1; // evita divisão por zero na normalização da cor
        for (Map.Entry<String, Integer> e : historicoVisitas.entrySet()) {
            int[] c = parseChave(e.getKey());
            if (c[0] > maxX) maxX = c[0];
            if (c[1] > maxY) maxY = c[1];
            if (e.getValue() > maxVisitas) maxVisitas = e.getValue();
        }

        int colunas = maxX + 1; // +1 porque as coordenadas começam em 0
        int linhas = maxY + 1;

        // 2) tamanho da célula: cabe sempre no painel, mantendo-a quadrada
        int lado = Math.min(getWidth() / colunas, getHeight() / linhas);
        if (lado < 1) lado = 1;

        // 3) desenhar cada célula visitada com cor consoante a temperatura
        for (Map.Entry<String, Integer> e : historicoVisitas.entrySet()) {
            int[] c = parseChave(e.getKey());
            g2.setColor(corTemperatura(e.getValue(), maxVisitas));
            int px = c[0] * lado;
            // Y invertido: linha 0 do ecrã corresponde ao topo, mas y=0 é o fundo da arena
            int py = (linhas - 1 - c[1]) * lado;
            g2.fillRect(px, py, lado, lado);
        }

        // 4) grelha por cima (linhas finas) para leitura das casas
        g2.setColor(new Color(40, 40, 40));
        for (int i = 0; i <= colunas; i++) g2.drawLine(i * lado, 0, i * lado, linhas * lado);
        for (int j = 0; j <= linhas; j++) g2.drawLine(0, j * lado, colunas * lado, j * lado);

        // 5) robô destacado na posição atual
        g2.setColor(Color.CYAN);
        int rx = xRobo * lado;
        int ry = (linhas - 1 - yRobo) * lado;
        g2.fillOval(rx, ry, lado, lado);
    }

    // "x,y" -> int[]{x, y}
    private int[] parseChave(String chave) {
        String[] partes = chave.split(",");
        return new int[]{ Integer.parseInt(partes[0]), Integer.parseInt(partes[1]) };
    }

    // interpola do frio (poucas visitas) ao quente (muitas visitas)
    private Color corTemperatura(int visitas, int maxVisitas) {
        float t = (float) visitas / maxVisitas; // 0.0 = frio, 1.0 = quente
        int r = (int) (t * 255);         // sobe com o calor
        int b = (int) ((1 - t) * 255);   // desce com o calor
        return new Color(r, 0, b);       // azul -> vermelho
    }
}