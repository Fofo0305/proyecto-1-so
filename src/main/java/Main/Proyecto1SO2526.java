package Main;

/**
 * Punto de entrada: abre la ventana principal en el EDT de Swing.
 *
 * Las comprobaciones que antes vivian comentadas en este archivo estan en
 * src/test/java/Pruebas como programas ejecutables: PruebaFlujoCompleto,
 * PruebaVentanaNormal y PruebaScrollColas.
 *
 * @author Adolfo
 */
public class Proyecto1SO2526 {

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new Interfaces.VentanaPrincipal().setVisible(true));
    }
}
