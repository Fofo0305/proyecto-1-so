package Interfaces;

import Modelo.VistaMetricasDisco;
import javax.swing.*;
import java.awt.*;

/**
 * Desplazamiento acumulado del cabezal (en bloques) contra el ciclo, con la serie que
 * guarda VistaMetricasDisco. Si se cambia la politica, la pendiente muestra cuanto se mueve
 * el cabezal con cada una.
 * @author Luciano
 */
public final class GraficaDesplazamiento extends JPanel {

    private long[] ciclos = new long[0];
    private long[] desplazamiento = new long[0];

    public GraficaDesplazamiento() {
        setPreferredSize(new Dimension(620, 220));
        setBackground(Color.WHITE);
    }

    public void mostrar(VistaMetricasDisco metricas) {
        ciclos = metricas == null ? new long[0] : metricas.ciclos();
        desplazamiento = metricas == null ? new long[0] : metricas.desplazamientoAcumulado();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics grafico) {
        super.paintComponent(grafico);
        Graphics2D lienzo = (Graphics2D) grafico.create();
        try {
            lienzo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = 60;
            int y = 30;
            int ancho = Math.max(1, getWidth() - 85);
            int alto = Math.max(1, getHeight() - 70);
            long minimo = ciclos.length == 0 ? 0 : ciclos[0];
            long maximo = ciclos.length == 0 ? 1 : Math.max(minimo + 1, ciclos[ciclos.length - 1]);
            long tope = 1;
            for (long valor : desplazamiento) tope = Math.max(tope, valor);

            lienzo.setColor(Color.DARK_GRAY);
            lienzo.drawString("Desplazamiento acumulado del cabezal (bloques)", x, 16);
            for (int marca = 0; marca <= 4; marca++) {
                int altura = y + alto - marca * alto / 4;
                lienzo.setColor(new Color(225, 230, 238));
                lienzo.drawLine(x, altura, x + ancho, altura);
                lienzo.setColor(Color.DARK_GRAY);
                lienzo.drawString("" + tope * marca / 4, 8, altura + 4);
            }
            lienzo.drawString("Ciclo " + minimo, x, y + alto + 18);
            lienzo.drawString("" + maximo, x + ancho - 40, y + alto + 18);
            if (ciclos.length < 2) {
                lienzo.drawString("Todavia no hay movimientos del cabezal.", x + 10, y + alto / 2);
                return;
            }
            lienzo.setColor(new Color(40, 110, 190));
            lienzo.setStroke(new BasicStroke(2));
            for (int punto = 1; punto < ciclos.length; punto++) {
                int desde = x + (int) ((ciclos[punto - 1] - minimo) * ancho / (maximo - minimo));
                int hasta = x + (int) ((ciclos[punto] - minimo) * ancho / (maximo - minimo));
                lienzo.drawLine(desde, y + alto - (int) (desplazamiento[punto - 1] * alto / tope),
                        hasta, y + alto - (int) (desplazamiento[punto] * alto / tope));
            }
        } finally {
            lienzo.dispose();
        }
    }
}
