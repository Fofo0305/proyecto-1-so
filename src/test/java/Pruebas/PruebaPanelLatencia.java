package Pruebas;

import Interfaces.PanelLatenciaRed;
import Modelo.Configuracion;
import Sistema.ControladorSimulacion;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;
import static Pruebas.PruebaSemaforos.verificar;

/** Cambio de latencia desde Swing y rechazo de entradas invalidas. @author Luciano Rojas */
public final class PruebaPanelLatencia {
    public static void main(String[] args) throws Exception {
        try (ControladorSimulacion c = new ControladorSimulacion()) {
            c.configurar(new Configuracion(new int[] {32,32},700,4,0));
            AtomicReference<String> mensaje = new AtomicReference<>("");
            PanelLatenciaRed[] panel = new PanelLatenciaRed[1];
            int[] aplicado = {-1};
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new PanelLatenciaRed(c, valor -> aplicado[0] = valor, mensaje::set);
                panel[0].mostrarLatencia(3);
                panel[0].aplicar();
            });
            long limite = System.nanoTime() + 5_000_000_000L;
            boolean listo = false;
            while (System.nanoTime() < limite) {
                boolean[] terminado = new boolean[1];
                SwingUtilities.invokeAndWait(() -> {
                    boolean habilitado = true;
                    for (var componente : panel[0].getComponents())
                        if (componente instanceof JButton boton) habilitado = boton.isEnabled();
                    terminado[0] = habilitado && aplicado[0] == 3;
                });
                if (terminado[0]) { listo = true; break; }
                Thread.sleep(10);
            }
            verificar(listo && c.getConfiguracion().getLatencia() == 3, "No aplico latencia desde Swing");
            for (String invalido : new String[] {"-1","1.5","2147483648","abc",""}) {
                SwingUtilities.invokeAndWait(() -> {
                    for (var componente : panel[0].getComponents())
                        if (componente instanceof JTextField campo) campo.setText(invalido);
                    panel[0].aplicar();
                });
                verificar(c.getConfiguracion().getLatencia() == 3 && mensaje.get().contains("entero"),
                        "Entrada invalida modifico la latencia: " + invalido);
            }
            System.out.println("OK: latencia desde Swing y validaciones sin cambios de sesion");
        }
    }
}
