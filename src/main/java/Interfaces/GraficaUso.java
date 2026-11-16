package Interfaces;

import Modelo.VistaMetricas;
import javax.swing.*;
import java.awt.*;

/**
 * Series de utilizacion acumulada por CPU, sobre el anillo de los ultimos 600 ciclos
 * que entrega cada VistaMetricas.
 * @author Antony
 */
public final class GraficaUso extends JPanel {

    private VistaMetricas[] vistas = new VistaMetricas[0];

    public GraficaUso() {
        setPreferredSize(new Dimension(800, 250));
        setBackground(Color.WHITE);
    }

    /** Reemplaza las series dibujadas y pide un repintado. */
    public void mostrar(VistaMetricas[] nuevas) {
        vistas = nuevas.clone();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics grafico) {
        super.paintComponent(grafico);
        Graphics2D lienzo = (Graphics2D) grafico.create();
        try {
            lienzo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = 55;
            int y = 35;
            int ancho = Math.max(1, getWidth() - 85);
            int alto = Math.max(1, getHeight() - 80);

            long minimo = Long.MAX_VALUE;
            long maximo = 1;
            for (VistaMetricas vista : vistas) {
                for (long ciclo : vista.historiaCiclos()) {
                    minimo = Math.min(minimo, ciclo);
                    maximo = Math.max(maximo, ciclo);
                }
            }
            if (minimo == Long.MAX_VALUE) {
                minimo = 0;
            }

            lienzo.setColor(Color.DARK_GRAY);
            lienzo.drawString("Utilizacion acumulada por CPU (%) - ultimos 600 ciclos", x, 18);
            for (int marca = 0; marca <= 4; marca++) {
                int altura = y + alto - marca * alto / 4;
                lienzo.setColor(new Color(225, 230, 238));
                lienzo.drawLine(x, altura, x + ancho, altura);
                lienzo.setColor(Color.DARK_GRAY);
                lienzo.drawString("" + (marca * 25), 10, altura + 4);
            }
            lienzo.drawString("Ciclo " + minimo, x, y + alto + 20);
            lienzo.drawString("" + maximo, x + ancho - 40, y + alto + 20);

            for (int indice = 0; indice < vistas.length; indice++) {
                VistaMetricas vista = vistas[indice];
                lienzo.setColor(Color.getHSBColor((float) indice / Math.max(1, vistas.length), .8f, .65f));
                lienzo.setStroke(new BasicStroke(2));
                lienzo.drawString("C" + vista.computador(), x + 400 + (indice % 6) * 48, 18 + (indice / 6) * 14);
                long[] ciclos = vista.historiaCiclos();
                double[] usos = vista.historiaUso();
                for (int punto = 1; punto < ciclos.length; punto++) {
                    int desde = x + (int) ((ciclos[punto - 1] - minimo) * ancho / Math.max(1, maximo - minimo));
                    int hasta = x + (int) ((ciclos[punto] - minimo) * ancho / Math.max(1, maximo - minimo));
                    lienzo.drawLine(desde, y + alto - (int) (usos[punto - 1] * alto / 100),
                            hasta, y + alto - (int) (usos[punto] * alto / 100));
                }
            }
        } finally {
            lienzo.dispose();
        }
    }
}
