package org.example.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;
import java.util.Set;

// Só desenha (SRP). Reflete a MEMÓRIA do cerebro: recebe conjuntos só-de-leitura
// (visitas, muros, recursos conhecidos, cofres falhados) + posição do robô.
public class PainelMapaCalor extends JPanel {

    private final Map<String, Integer> historicoVisitas;
    private final Set<String> murosConhecidos;
    private final Set<String> recursosConhecidos;
    private final Set<String> cofresFalhados;

    private int xRobo = 0;
    private int yRobo = 0;

    private static final Color COR_ANDADA = new Color(60, 90, 160);
    private static final Color COR_MURO   = new Color(150, 90, 30);

    // recebe as referências só-de-leitura do cerebro (partilhadas, refletem sempre o estado atual)
    public PainelMapaCalor(Map<String, Integer> historicoVisitas,
                           Set<String> murosConhecidos,
                           Set<String> recursosConhecidos,
                           Set<String> cofresFalhados) {
        this.historicoVisitas = historicoVisitas;
        this.murosConhecidos = murosConhecidos;
        this.recursosConhecidos = recursosConhecidos;
        this.cofresFalhados = cofresFalhados;
        setBackground(Color.BLACK);
    }

    // só posição do robô muda por chamada; o resto é lido dos conjuntos partilhados
    public void atualizar(int x, int y) {
        this.xRobo = x;
        this.yRobo = y;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int maxX = xRobo, maxY = yRobo;
        for (String k : historicoVisitas.keySet())   { int[] c = parseChave(k); maxX = Math.max(maxX, c[0]); maxY = Math.max(maxY, c[1]); }
        for (String k : murosConhecidos)             { int[] c = parseChave(k); maxX = Math.max(maxX, c[0]); maxY = Math.max(maxY, c[1]); }
        for (String k : recursosConhecidos)          { int[] c = parseChave(k); maxX = Math.max(maxX, c[0]); maxY = Math.max(maxY, c[1]); }
        for (String k : cofresFalhados)              { int[] c = parseChave(k); maxX = Math.max(maxX, c[0]); maxY = Math.max(maxY, c[1]); }

        int colunas = maxX + 1, linhas = maxY + 1;
        int lado = Math.min(getWidth() / colunas, getHeight() / linhas);
        if (lado < 1) lado = 1;

        // casas andadas: cor única + número de visitas
        g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(8, lado / 3)));
        for (Map.Entry<String, Integer> e : historicoVisitas.entrySet()) {
            int[] c = parseChave(e.getKey());
            int px = c[0] * lado, py = (linhas - 1 - c[1]) * lado;
            g2.setColor(COR_ANDADA);
            g2.fillRect(px, py, lado, lado);
            desenharTextoCentrado(g2, String.valueOf(e.getValue()), px, py, lado, Color.WHITE);
        }

        // muros (castanho) — memória do motor
        g2.setColor(COR_MURO);
        for (String k : murosConhecidos) {
            int[] c = parseChave(k);
            g2.fillRect(c[0] * lado, (linhas - 1 - c[1]) * lado, lado, lado);
        }

        // recursos conhecidos (verde) — somem sozinhos quando o cerebro os remove (coletados)
        g2.setColor(Color.GREEN);
        for (String k : recursosConhecidos) {
            int[] c = parseChave(k);
            g2.fillRect(c[0] * lado, (linhas - 1 - c[1]) * lado, lado, lado);
        }

        // cofres falhados (vermelho) com "F" central
        for (String k : cofresFalhados) {
            int[] c = parseChave(k);
            int px = c[0] * lado, py = (linhas - 1 - c[1]) * lado;
            g2.setColor(new Color(120, 30, 30));
            g2.fillRect(px, py, lado, lado);
            desenharTextoCentrado(g2, "F", px, py, lado, Color.WHITE);
        }

        // grelha
        g2.setColor(new Color(40, 40, 40));
        for (int i = 0; i <= colunas; i++) g2.drawLine(i * lado, 0, i * lado, linhas * lado);
        for (int j = 0; j <= linhas; j++) g2.drawLine(0, j * lado, colunas * lado, j * lado);

        // robô (ciano)
        g2.setColor(Color.CYAN);
        g2.fillOval(xRobo * lado, (linhas - 1 - yRobo) * lado, lado, lado);
    }

    private void desenharTextoCentrado(Graphics2D g2, String txt, int px, int py, int lado, Color cor) {
        g2.setColor(cor);
        int tw = g2.getFontMetrics().stringWidth(txt);
        int th = g2.getFontMetrics().getAscent();
        g2.drawString(txt, px + (lado - tw) / 2, py + (lado + th) / 2);
    }

    private int[] parseChave(String chave) {
        String[] partes = chave.split(",");
        return new int[]{ Integer.parseInt(partes[0]), Integer.parseInt(partes[1]) };
    }
}