package Pruebas;

import Interfaces.PanelDisco;
import Modelo.*;
import Sistema.ControladorSimulacion;
import java.awt.*;
import java.util.concurrent.Callable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultTreeModel;

/**
 * Pestana "Disco y archivos": politica de disco, CRUD desde el formulario, arbol y tabla
 * de asignacion, y un intento prohibido que se rechaza sin detener la simulacion.
 * @author Luciano
 */
public final class PruebaPanelDisco {
    public static void main(String[] args) throws Exception {
        try (ControladorSimulacion controlador = new ControladorSimulacion()) {
            controlador.configurar(new Configuracion(new int[] {32}, 10, 4));
            PanelDisco[] panel = new PanelDisco[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new PanelDisco(controlador);
                campo(panel[0], "politicaDisco", JComboBox.class).setSelectedItem(PoliticaDisco.SSTF);
                boton(panel[0], "Aplicar politica de disco").doClick();
            });
            esperar(() -> controlador.getDisco().getPolitica() == PoliticaDisco.SSTF, "politica de disco");

            SwingUtilities.invokeAndWait(() -> boton(panel[0], "Crear proceso de archivo").doClick());
            esperar(() -> controlador.getCargaInicial().getProcesos().length == 1, "proceso CREAR");
            controlador.iniciar();
            esperar(() -> controlador.consultarAlmacenamiento().asignacion().length == 1, "archivo creado");

            DefaultTreeModel arbol = campo(panel[0], "modeloArbol", DefaultTreeModel.class);
            DefaultTableModel asignacion = campo(panel[0], "modeloAsignacion", DefaultTableModel.class);
            esperar(() -> {
                panel[0].actualizar();
                return enEdt(() -> asignacion.getRowCount() == 1 && arbol.getChildCount(arbol.getRoot()) == 1
                        && "notas.txt".equals(asignacion.getValueAt(0, 0)));
            }, "arbol y tabla de asignacion");

            SwingUtilities.invokeAndWait(() -> {
                campo(panel[0], "modo", JComboBox.class).setSelectedItem(ModoUsuario.USUARIO);
                campo(panel[0], "usuario", JTextField.class).setText("ana");
                boton(panel[0], "Cambiar sesion").doClick();
            });
            esperar(() -> "ana".equals(controlador.getSistemaArchivos().getUsuario()), "sesion de ana");

            JLabel resultado = campo(panel[0], "resultado", JLabel.class);
            SwingUtilities.invokeAndWait(() -> {
                campo(panel[0], "ruta", JTextField.class).setText("/prohibido.txt");
                boton(panel[0], "Crear proceso de archivo").doClick();
            });
            esperar(() -> enEdt(() -> resultado.getText().contains("solo puede crear archivos en sus propios directorios")
                    && resultado.getText().startsWith("Rechazado")), "rechazo visible");

            long ciclo = controlador.getCiclo();
            esperar(() -> controlador.getCiclo() > ciclo, "ciclo despues del rechazo");
            if (controlador.getEstado().startsWith("Error"))
                throw new AssertionError("El rechazo detuvo la simulacion: " + controlador.getEstado());
        }
        System.out.println("OK: disco y sistema de archivos desde Swing, con rechazo visible");
    }

    private static <T> T campo(Object panel, String nombre, Class<T> tipo) {
        try {
            var campo = PanelDisco.class.getDeclaredField(nombre);
            campo.setAccessible(true);
            return tipo.cast(campo.get(panel));
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
