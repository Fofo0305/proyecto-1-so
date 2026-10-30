package Pruebas;

/**
 * Recorre el flujo completo desde las acciones Swing: carga el ejemplo, crea un proceso
 * desde el formulario, cambia la duracion del ciclo, inicia/pausa/reanuda/detiene,
 * comprueba finalizacion y RAM, guarda y abre CSV, rechaza un archivo corrupto sin perder
 * la sesion y verifica que no queden trabajadores del reloj al cerrar.
 */
public final class PruebaFlujoCompleto {

    public static void main(String[] args) throws Exception {
        Interfaces.VentanaPrincipal[] ventanaE2E = new Interfaces.VentanaPrincipal[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ventanaE2E[0] = new Interfaces.VentanaPrincipal();
                ventanaE2E[0].setSize(900, 680); ventanaE2E[0].setVisible(true);
                ventanaE2E[0].getPanelArchivos().cargarEjemplo();
            });
            Sistema.ControladorSimulacion controlE2E = ventanaE2E[0].getControlador();
            String[] mensajeE2E = {""};
            long limiteE2E = System.nanoTime() + 3_000_000_000L;
            do {
                javax.swing.SwingUtilities.invokeAndWait(() -> mensajeE2E[0] = ventanaE2E[0].getPanelArchivos().getResultado());
                if (mensajeE2E[0].startsWith("Ejemplo listo")) break;
                Thread.sleep(5);
            } while (System.nanoTime() < limiteE2E);
            if (!mensajeE2E[0].startsWith("Ejemplo listo")) throw new AssertionError("No cargo el ejemplo: " + mensajeE2E[0]);
            if (controlE2E.getComputadores()[0].getNuevos().length != 1) throw new AssertionError("No hay espera inicial por RAM");
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ventanaE2E[0].mostrarProcesos();
                javax.swing.JPanel formularioE2E = (javax.swing.JPanel) ventanaE2E[0].getPanelProceso().getComponent(1);
                for (java.awt.Component componenteE2E : formularioE2E.getComponents()) {
                    if (componenteE2E instanceof javax.swing.JTextField campoE2E) {
                        switch (campoE2E.getName()) {
                            case "Nombre" -> campoE2E.setText("Manual E2E");
                            case "Computador" -> campoE2E.setText("2");
                            case "Instrucciones" -> campoE2E.setText("2");
                            case "Memoria" -> campoE2E.setText("16");
                            case "Prioridad" -> campoE2E.setText("1");
                            case "Deadline" -> campoE2E.setText("80");
                        }
                    }
                }
                ventanaE2E[0].crearProceso();
            });
            limiteE2E = System.nanoTime() + 3_000_000_000L;
            do {
                javax.swing.SwingUtilities.invokeAndWait(() -> mensajeE2E[0] = ventanaE2E[0].getEstado());
                if (mensajeE2E[0].startsWith("Proceso ")) break;
                Thread.sleep(5);
            } while (System.nanoTime() < limiteE2E);
            if (!mensajeE2E[0].startsWith("Proceso ") || controlE2E.getCargaInicial().getProcesos().length != 6)
                throw new AssertionError("No creo proceso desde formulario: " + mensajeE2E[0]);
            System.out.println("E2E: ejemplo cargado y sexto proceso creado desde el formulario: OK");

            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ventanaE2E[0].getPanelControles().mostrarDuracion(30);
                ventanaE2E[0].getPanelControles().ejecutarAccion("ciclo");
            });
            limiteE2E = System.nanoTime() + 3_000_000_000L;
            while (controlE2E.getConfiguracion().getDuracionMs() != 30 && System.nanoTime() < limiteE2E) Thread.sleep(5);
            if (controlE2E.getConfiguracion().getDuracionMs() != 30) throw new AssertionError("No cambio ciclo desde interfaz");
            javax.swing.SwingUtilities.invokeAndWait(() -> ventanaE2E[0].getPanelControles().ejecutarAccion("iniciar"));
            limiteE2E = System.nanoTime() + 3_000_000_000L;
            while (controlE2E.getCiclo() < 2 && System.nanoTime() < limiteE2E) Thread.sleep(5);
            if (controlE2E.getCiclo() < 2) throw new AssertionError("Reloj no avanza");
            javax.swing.SwingUtilities.invokeAndWait(() -> ventanaE2E[0].getPanelControles().ejecutarAccion("pausar"));
            limiteE2E = System.nanoTime() + 3_000_000_000L;
            while (!"Pausado".equals(controlE2E.getEstado()) && System.nanoTime() < limiteE2E) Thread.sleep(5);
            if (!"Pausado".equals(controlE2E.getEstado())) throw new AssertionError("No pauso");
            long cicloPausadoE2E = controlE2E.getCiclo(); Thread.sleep(80);
            if (controlE2E.getCiclo() != cicloPausadoE2E) throw new AssertionError("El reloj avanzo pausado");
            javax.swing.SwingUtilities.invokeAndWait(() -> ventanaE2E[0].getPanelControles().ejecutarAccion("reanudar"));
            limiteE2E = System.nanoTime() + 5_000_000_000L;
            while (controlE2E.getComputadores()[0].getTerminados().length + controlE2E.getComputadores()[1].getTerminados().length < 6
                    && System.nanoTime() < limiteE2E) Thread.sleep(5);
            javax.swing.SwingUtilities.invokeAndWait(() -> ventanaE2E[0].getPanelControles().ejecutarAccion("detener"));
            limiteE2E = System.nanoTime() + 3_000_000_000L;
            while (!"Detenido".equals(controlE2E.getEstado()) && System.nanoTime() < limiteE2E) Thread.sleep(5);
            Sistema.Computador[] equiposE2E = controlE2E.getComputadores();
            if (!"Detenido".equals(controlE2E.getEstado()) || equiposE2E[0].getTerminados().length != 3 || equiposE2E[1].getTerminados().length != 3
                    || equiposE2E[0].getMemoria().getUsada() != 8 || equiposE2E[1].getMemoria().getUsada() != 0)
                throw new AssertionError("Finalizacion/readmision o RAM incorrecta");
            for (Sistema.Computador equipoE2E : equiposE2E)
                for (Modelo.Proceso procesoE2E : equipoE2E.getTerminados())
                    if (procesoE2E.getRestante() != 0 || procesoE2E.getPc() != procesoE2E.getInstrucciones())
                        throw new AssertionError("Proceso vencio o no completo sus instrucciones");
            System.out.println("E2E: iniciar/pausar/reanudar/detener, 6 completados y RAM libre salvo buffer de 8: OK");

            java.nio.file.Path archivoE2E = java.nio.file.Path.of("target", "end-to-end.csv");
            for (boolean guardarE2E : new boolean[] {true, false}) {
                javax.swing.SwingUtilities.invokeAndWait(() -> ventanaE2E[0].getPanelArchivos().procesarArchivo(archivoE2E, guardarE2E));
                String esperadoE2E = guardarE2E ? "Guardado:" : "Cargado:";
                limiteE2E = System.nanoTime() + 3_000_000_000L;
                do {
                    javax.swing.SwingUtilities.invokeAndWait(() -> mensajeE2E[0] = ventanaE2E[0].getPanelArchivos().getResultado());
                    if (mensajeE2E[0].startsWith(esperadoE2E)) break;
                    Thread.sleep(5);
                } while (System.nanoTime() < limiteE2E);
                if (!mensajeE2E[0].startsWith(esperadoE2E)) throw new AssertionError("Archivo fallo: " + mensajeE2E[0]);
            }
            if (controlE2E.getCiclo() != 0 || controlE2E.getCargaInicial().getProcesos().length != 6
                    || controlE2E.getComputadores()[0].getNuevos().length != 1 || controlE2E.getComputadores()[1].getListos().length != 3)
                throw new AssertionError("Carga CSV no restauro las condiciones iniciales");
            if (controlE2E.getCargaInicial().getProcesos()[0].deadline() != 80 || controlE2E.getComputadores()[0].getListos()[0].getPc() != 0)
                throw new AssertionError("Guardo contexto de ejecucion en vez de carga inicial");
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                if (!"Preparado".equals(ventanaE2E[0].getPanelControles().getResultado())
                        || !ventanaE2E[0].getEstado().contains("Carga inicial lista"))
                    throw new AssertionError("La interfaz conserva el estado de la sesion anterior");
            });
            Sistema.Computador sesionE2E = controlE2E.getComputadores()[0];
            java.nio.file.Path corruptoE2E = java.nio.file.Path.of("target", "end-to-end-invalido.csv");
            java.nio.file.Files.writeString(corruptoE2E, "AVILAOS,999\n");
            javax.swing.SwingUtilities.invokeAndWait(() -> ventanaE2E[0].getPanelArchivos().procesarArchivo(corruptoE2E, false));
            limiteE2E = System.nanoTime() + 3_000_000_000L;
            do {
                javax.swing.SwingUtilities.invokeAndWait(() -> mensajeE2E[0] = ventanaE2E[0].getPanelArchivos().getResultado());
                if (mensajeE2E[0].startsWith("Error:")) break;
                Thread.sleep(5);
            } while (System.nanoTime() < limiteE2E);
            if (!mensajeE2E[0].startsWith("Error:") || controlE2E.getComputadores()[0] != sesionE2E)
                throw new AssertionError("CSV corrupto altero sesion o no mostro error");
            System.out.println("E2E: guardar/abrir desde interfaz restaura ciclo y PC cero; CSV corrupto conserva sesion: OK");
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
        catch (java.io.IOException | java.lang.reflect.InvocationTargetException error) { throw new IllegalStateException(error); }
        finally {
            if (ventanaE2E[0] != null) {
                ventanaE2E[0].getControlador().close();
                try { javax.swing.SwingUtilities.invokeAndWait(() -> ventanaE2E[0].dispose()); }
                catch (Exception error) { throw new IllegalStateException(error); }
            }
        }
        Thread[] hilosE2E = new Thread[Thread.activeCount() * 2 + 16];
        int cantidadHilosE2E = Thread.enumerate(hilosE2E);
        for (int hiloE2E = 0; hiloE2E < cantidadHilosE2E; hiloE2E++)
            if (hilosE2E[hiloE2E].isAlive() && (hilosE2E[hiloE2E].getName().equals("Reloj-global") || hilosE2E[hiloE2E].getName().startsWith("Computador-reloj-")))
                throw new AssertionError("Hilo del reloj abandonado");
        System.out.println("E2E: ventana y trabajadores del reloj cerrados: OK");
    }
}
