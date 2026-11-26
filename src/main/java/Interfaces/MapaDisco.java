package Interfaces;

import Modelo.AreaDisco;
import Modelo.VistaAlmacenamiento;
import Modelo.VistaBloqueDisco;
import javax.swing.*;
import java.awt.*;

/**
 * Mapa de bloques del disco: cada bloque ocupado lleva el color de su duenio (el mismo
 * de la tabla de asignacion), el area de intercambio va tenida de lila, el cabezal se marca
 * en rojo y el bloque que busca la solicitud activa en naranja.
 * @author Luciano
 */
public final class MapaDisco extends JPanel {

    private static final Color LIBRE = new Color(236, 239, 244);
    private static final Color SWAP_LIBRE = new Color(232, 224, 246);
    private static final Color CABEZAL = new Color(210, 40, 40);
    private static final Color OBJETIVO = new Color(240, 140, 20);

    private VistaAlmacenamiento vista;

    public MapaDisco() {
        setPreferredSize(new Dimension(620, 230));
        setBackground(Color.WHITE);
    }

    /** Color estable para un duenio del disco: el mismo texto siempre da el mismo color. */
    public static Color colorDe(String etiqueta) {
        int hash = etiqueta.hashCode() * 0x9E3779B1;
        float tono = ((hash >>> 8) % 360) / 360f;
        return Color.getHSBColor(tono, 0.45f, 0.93f);
    }

    public void mostrar(VistaAlmacenamiento nueva) {
        vista = nueva;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics grafico) {
        super.paintComponent(grafico);
        Graphics2D lienzo = (Graphics2D) grafico.create();
        try {
            lienzo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            lienzo.setColor(Color.DARK_GRAY);
            if (vista == null) {
                lienzo.drawString("Configure una simulacion para ver el disco.", 12, 20);
                return;
            }
            VistaBloqueDisco[] bloques = vista.bloques();
            int objetivo = vista.activa() == null ? -1 : vista.activa().bloqueObjetivo();
            String flecha = vista.direccion() == Modelo.DireccionDisco.ASCENDENTE ? "→ ascendente" : "← descendente";
            lienzo.setFont(getFont().deriveFont(Font.BOLD));
            lienzo.drawString("Politica " + vista.politica() + "   |   Cabezal en el bloque " + vista.cabezal()
                    + "   |   Direccion " + flecha + "   |   Swap: bloques " + (bloques.length - vista.tamanoSwap())
                    + "-" + (bloques.length - 1), 10, 16);
            lienzo.setFont(getFont());

            // Elige las columnas que dan el bloque mas grande en el espacio disponible.
            int columnas = 10;
            int lado = 0;
            for (int prueba = 10; prueba <= 50; prueba++) {
                int filasPrueba = (bloques.length + prueba - 1) / prueba;
                int ladoPrueba = Math.min((getWidth() - 20) / prueba, (getHeight() - 46) / Math.max(1, filasPrueba));
                if (ladoPrueba > lado) {
                    lado = ladoPrueba;
                    columnas = prueba;
                }
            }
            lado = Math.max(6, lado);
            int filas = (bloques.length + columnas - 1) / columnas;
            boolean numeros = lado >= 22;
            lienzo.setFont(getFont().deriveFont(Math.min(11f, lado / 2.4f)));
            FontMetrics medidas = lienzo.getFontMetrics();
            for (VistaBloqueDisco bloque : bloques) {
                int x = 10 + (bloque.numero() % columnas) * lado;
                int y = 26 + (bloque.numero() / columnas) * lado;
                Color relleno = bloque.libre() ? (bloque.area() == AreaDisco.SWAP ? SWAP_LIBRE : LIBRE)
                        : colorDe(bloque.propietario());
                lienzo.setColor(relleno);
                lienzo.fillRect(x + 1, y + 1, lado - 2, lado - 2);
                lienzo.setColor(bloque.area() == AreaDisco.SWAP ? new Color(150, 120, 200) : new Color(190, 196, 206));
                lienzo.drawRect(x + 1, y + 1, lado - 3, lado - 3);
                if (numeros) {
                    String texto = "" + bloque.numero();
                    lienzo.setColor(new Color(60, 66, 80));
                    lienzo.drawString(texto, x + (lado - medidas.stringWidth(texto)) / 2, y + lado / 2 + medidas.getAscent() / 2 - 1);
                }
                if (bloque.numero() == objetivo) {
                    lienzo.setColor(OBJETIVO);
                    lienzo.setStroke(new BasicStroke(2.5f));
                    lienzo.drawRect(x + 2, y + 2, lado - 5, lado - 5);
                }
                if (bloque.numero() == vista.cabezal()) {
                    lienzo.setColor(CABEZAL);
                    lienzo.setStroke(new BasicStroke(3f));
                    lienzo.drawRect(x, y, lado - 1, lado - 1);
                }
                lienzo.setStroke(new BasicStroke(1f));
            }
            int pie = 26 + filas * lado + 14;
            lienzo.setFont(getFont());
            int x = leyenda(lienzo, 10, pie, CABEZAL, "Cabezal", true);
            x = leyenda(lienzo, x, pie, OBJETIVO, "Bloque que busca la solicitud activa", true);
            x = leyenda(lienzo, x, pie, LIBRE, "Libre", false);
            x = leyenda(lienzo, x, pie, SWAP_LIBRE, "Swap libre", false);
            lienzo.setColor(Color.DARK_GRAY);
            lienzo.drawString("Color = duenio del bloque (archivo o imagen P# en swap)", x, pie);
        } finally {
            lienzo.dispose();
        }
    }

    /** Dibuja una entrada de la leyenda y devuelve donde empieza la siguiente. */
    private static int leyenda(Graphics2D lienzo, int x, int y, Color color, String texto, boolean borde) {
        lienzo.setColor(color);
        if (borde) {
            lienzo.setStroke(new BasicStroke(2.5f));
            lienzo.drawRect(x, y - 10, 11, 11);
            lienzo.setStroke(new BasicStroke(1f));
        } else {
            lienzo.fillRect(x, y - 10, 12, 12);
        }
        lienzo.setColor(Color.DARK_GRAY);
        lienzo.drawString(texto, x + 16, y);
        return x + 16 + lienzo.getFontMetrics().stringWidth(texto) + 18;
    }
}
