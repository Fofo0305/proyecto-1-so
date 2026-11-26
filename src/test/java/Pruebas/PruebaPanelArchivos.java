package Pruebas;

import Interfaces.PanelArchivos;
import Interfaces.PanelControles;
import Modelo.Configuracion;
import Sistema.ControladorSimulacion;
import java.awt.*;
import java.util.concurrent.Callable;
import javax.swing.*;

/**
 * Botones de los JSON: Iniciar se niega y dice que falta mientras no esten los dos archivos,
 * y "Cargar ejemplo" deja el disco y los procesos del enunciado listos para iniciar.
 * @author Luciano
 */
public final class PruebaPanelArchivos {
    public static void main(String[] args) throws Exception {
        try (ControladorSimulacion controlador = new ControladorSimulacion()) {
            controlador.configurar(new Configuracion(new int[] {128}, 10, 4));
            PanelArchivos[] archivos = new PanelArchivos[1];
            PanelControles[] controles = new PanelControles[1];
            SwingUtilities.invokeAndWait(() -> {
                archivos[0] = new PanelArchivos(controlador, carga -> { });
                controles[0] = new PanelControles(controlador);
                for (String texto : new String[] {"Abrir JSON del disco", "Abrir JSON de procesos", "Guardar JSON del disco"})
                    boton(archivos[0], texto);
            });
            JLabel faltante = campo(PanelArchivos.class, archivos[0], "faltante");
            esperar(() -> enEdt(() -> faltante.getText().equals("Faltan el JSON del disco y el JSON de procesos")),
                    "aviso de los dos JSON");

            JLabel resultadoControles = campo(PanelControles.class, controles[0], "resultado");
            SwingUtilities.invokeAndWait(() -> controles[0].ejecutarAccion("iniciar"));
            esperar(() -> enEdt(() -> resultadoControles.getText().equals("Faltan el JSON del disco y el JSON de procesos")),
                    "Iniciar rechazado sin los JSON");
            if (controlador.getCiclo() != 0) throw new AssertionError("Inicio sin los dos JSON");

            SwingUtilities.invokeAndWait(() -> boton(archivos[0], "Cargar ejemplo").doClick());
            JLabel resultado = campo(PanelArchivos.class, archivos[0], "resultado");
            esperar(() -> enEdt(() -> resultado.getText().startsWith("Ejemplo del enunciado listo")
                    && resultado.getText().contains("200 bloques") && resultado.getText().contains("cabezal en 50")
                    && faltante.getText().startsWith("Listo")), "ejemplo cargado");
            if (controlador.getCargaInicial().getProcesos().length != 8)
                throw new AssertionError("El ejemplo no trajo los ocho procesos");

            SwingUtilities.invokeAndWait(() -> controles[0].ejecutarAccion("iniciar"));
            esperar(() -> controlador.getCiclo() > 0, "Iniciar con los dos JSON");
        }
        System.out.println("OK: Iniciar espera los dos JSON y el ejemplo los carga desde Swing");
    }

    private static <T> T campo(Class<?> clase, Object panel, String nombre) {
        try {
            var campo = clase.getDeclaredField(nombre);
            campo.setAccessible(true);
            @SuppressWarnings("unchecked")
            T valor = (T) campo.get(panel);
            return valor;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static boolean enEdt(Callable<Boolean> condicion) throws Exception {
        boolean[] valor = new boolean[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                valor[0] = condicion.call();
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        });
        return valor[0];
    }

    private static JButton boton(Container panel, String texto) {
        JButton encontrado = buscar(panel, texto);
        if (encontrado == null) throw new AssertionError("No hay boton " + texto);
        return encontrado;
    }

    private static JButton buscar(Container panel, String texto) {
        for (Component componente : panel.getComponents()) {
            if (componente instanceof JButton boton && boton.getText().equals(texto)) return boton;
            if (componente instanceof Container contenedor) {
                JButton encontrado = buscar(contenedor, texto);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }

    private static void esperar(Callable<Boolean> condicion, String que) throws Exception {
        long limite = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < limite) {
            if (condicion.call()) return;
            Thread.sleep(20);
        }
        throw new AssertionError("No se cumplio: " + que);
    }
}
