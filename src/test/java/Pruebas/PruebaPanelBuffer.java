package Pruebas;

import Interfaces.PanelBuffer;
import Modelo.Configuracion;
import Sistema.ControladorSimulacion;
import javax.swing.SwingUtilities;

/** Verifica solicitudes de formulario sin depender de ventanas. @author Luciano Rojas */
public final class PruebaPanelBuffer {
    private static void rechazar(Runnable accion) {
        try {
            accion.run();
            throw new AssertionError("Se acepto una solicitud invalida");
        } catch (IllegalArgumentException esperado) { }
    }

    public static void main(String[] args) throws Exception {
        String[][] solicitudes = {
            null, {}, {"x"}, {" ", "1", "1"}, {null, "1", "1"},
            {"x", "0", "1"}, {"x", "-1", "1"}, {"x", "1.5", "1"},
            {"x", "2147483648", "1"}, {"x", null, "1"},
            {"x", "1", "0"}, {"x", "1", "abc"}
        };
        for (String[] solicitud : solicitudes) rechazar(() -> PanelBuffer.validarCampos(solicitud));
        try (ControladorSimulacion controlador = new ControladorSimulacion()) {
            controlador.configurar(new Configuracion(new int[] {16, 16}, 700, 4, 0));
            String[][] campos = new String[1][];
            SwingUtilities.invokeAndWait(() -> campos[0] = new PanelBuffer().leerCampos());
            PanelBuffer.crearBuffer(controlador, campos[0]);
            rechazar(() -> PanelBuffer.crearBuffer(controlador, campos[0]));
            rechazar(() -> PanelBuffer.crearBuffer(controlador, new String[] {"Otro", "1", "3"}));
            rechazar(() -> PanelBuffer.crearBuffer(controlador, new String[] {"Grande", "9", "1"}));
            if (controlador.getBuffers().length != 1 || controlador.getComputadores()[0].getMemoria().getUsada() != 8)
                throw new AssertionError("Las solicitudes rechazadas modificaron la sesion");
        }
        System.out.println("OK: formulario, tipos, rangos y errores sin cambios en la sesion");
    }
}
