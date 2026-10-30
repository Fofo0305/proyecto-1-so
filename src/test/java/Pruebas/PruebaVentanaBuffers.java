package Pruebas;

import Interfaces.VentanaPrincipal;
import Modelo.Configuracion;
import javax.swing.SwingUtilities;

/** Flujo del boton de buffers sin mostrar ventanas. Requiere escritorio. @author Luciano Rojas */
public final class PruebaVentanaBuffers {
    public static void main(String[] args) throws Exception {
        VentanaPrincipal[] ventana = new VentanaPrincipal[1];
        SwingUtilities.invokeAndWait(() -> ventana[0] = new VentanaPrincipal());
        try {
            ventana[0].getControlador().configurar(new Configuracion(new int[] {16,16},700,4,0));
            SwingUtilities.invokeAndWait(() -> ventana[0].crearBuffer());
            esperarResultado(ventana[0]);
            if (ventana[0].getControlador().getBuffers().length != 1
                    || ventana[0].getControlador().getComputadores()[0].getMemoria().getUsada() != 8)
                throw new AssertionError("El boton no registro el buffer con su reserva");
            String[] estado = new String[1];
            SwingUtilities.invokeAndWait(() -> estado[0] = ventana[0].getEstado());
            if (!estado[0].contains("RAM reservada: 8"))
                throw new AssertionError("Falta confirmacion de la reserva: " + estado[0]);
            SwingUtilities.invokeAndWait(() -> ventana[0].crearBuffer());
            esperarResultado(ventana[0]);
            SwingUtilities.invokeAndWait(() -> estado[0] = ventana[0].getEstado());
            if (!estado[0].contains("duplicado") || ventana[0].getControlador().getBuffers().length != 1)
                throw new AssertionError("No se gestiono el nombre duplicado");
            System.out.println("OK: creacion desde ventana, confirmacion y duplicado sin cambios");
        } finally {
            ventana[0].getControlador().close();
            SwingUtilities.invokeAndWait(() -> ventana[0].dispose());
        }
    }

    private static void esperarResultado(VentanaPrincipal ventana) throws Exception {
        long limite = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < limite) {
            boolean[] pendiente = new boolean[1];
            SwingUtilities.invokeAndWait(() -> pendiente[0] = ventana.getEstado().equals("Creando buffer..."));
            if (!pendiente[0]) return;
            Thread.sleep(20);
        }
        throw new AssertionError("La creacion de buffer no termino en cinco segundos");
    }
}
