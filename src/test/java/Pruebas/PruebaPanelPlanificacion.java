package Pruebas;

import Interfaces.PanelPlanificacion;
import Modelo.*;
import Sistema.ControladorSimulacion;
import java.awt.*;
import javax.swing.*;

/** Acciones Swing de política y creación de E/S en el computador único. */
public final class PruebaPanelPlanificacion {
    public static void main(String[] args) throws Exception {
        try (ControladorSimulacion controlador = new ControladorSimulacion()) {
            controlador.configurar(new Configuracion(new int[] {32}, 10, 4));
            PanelPlanificacion[] panel = new PanelPlanificacion[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new PanelPlanificacion(controlador);
                boton(panel[0], "Crear proceso de E/S").doClick();
            });
            esperar(() -> controlador.getCargaInicial().getProcesos().length == 1);
            if (!controlador.getCargaInicial().getProcesos()[0].parametrosES().equals(new ParametrosES(2, 3)))
                throw new AssertionError("Formulario pierde E/S");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    var campo = PanelPlanificacion.class.getDeclaredField("politica");
                    campo.setAccessible(true);
                    ((JComboBox<?>) campo.get(panel[0])).setSelectedItem(PoliticaPlanificacion.ROUND_ROBIN);
                    boton(panel[0], "Aplicar politica y quantum").doClick();
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            esperar(() -> controlador.getComputadores()[0].getPolitica() == PoliticaPlanificacion.ROUND_ROBIN);
        }
        System.out.println("OK: politica y E/S desde Swing en un computador");
    }

    private static JButton boton(Container panel, String texto) {
        for (Component componente : panel.getComponents()) {
            if (componente instanceof JButton boton && boton.getText().equals(texto)) return boton;
            if (componente instanceof Container contenedor) {
                JButton encontrado = boton(contenedor, texto);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }

    private static void esperar(java.util.concurrent.Callable<Boolean> condicion) throws Exception {
        long limite = System.nanoTime() + 3_000_000_000L;
        while (System.nanoTime() < limite) {
            if (condicion.call()) return;
            Thread.sleep(10);
        }
        throw new AssertionError("Solicitud Swing no termino");
    }
}
