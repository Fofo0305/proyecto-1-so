package Main;

/**
 *
 * @author Adolfo
 */
public class Proyecto1SO2526 {

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new Interfaces.VentanaPrincipal().setVisible(true));

        /* Prueba end to end verificada en consola.
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
        */
        /* Prueba de ventana sin maximizar y formulario de procesos verificada en consola.
        Interfaces.VentanaPrincipal[] ventanaNormal = new Interfaces.VentanaPrincipal[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ventanaNormal[0] = new Interfaces.VentanaPrincipal();
                ventanaNormal[0].setSize(900, 680); ventanaNormal[0].setVisible(true); ventanaNormal[0].validate();
                if ((ventanaNormal[0].getExtendedState() & java.awt.Frame.MAXIMIZED_BOTH) != 0)
                    throw new AssertionError("La ventana requiere maximizarse");
                java.awt.Rectangle areaNormal = new java.awt.Rectangle(ventanaNormal[0].getContentPane().getSize());
                Interfaces.PanelControles controlesNormal = ventanaNormal[0].getPanelControles();
                javax.swing.JPanel accionesNormal = (javax.swing.JPanel) controlesNormal.getComponent(0);
                for (java.awt.Component botonNormal : accionesNormal.getComponents()) {
                    java.awt.Rectangle limitesNormal = javax.swing.SwingUtilities.convertRectangle(botonNormal.getParent(), botonNormal.getBounds(), ventanaNormal[0].getContentPane());
                    if (!areaNormal.contains(limitesNormal)) throw new AssertionError("Control fuera de ventana: " + limitesNormal);
                }
                ventanaNormal[0].mostrarProcesos(); ventanaNormal[0].validate();
                Interfaces.PanelProceso formularioNormal = ventanaNormal[0].getPanelProceso();
                String[] camposNormal = formularioNormal.leerCampos();
                if (!"Proceso".equals(camposNormal[0]) || !"32".equals(camposNormal[3])) throw new AssertionError("Formulario cambia valores");
                java.awt.image.BufferedImage imagenNormal = new java.awt.image.BufferedImage(900, 680, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D pincelNormal = imagenNormal.createGraphics();
                try { ventanaNormal[0].printAll(pincelNormal); } finally { pincelNormal.dispose(); }
                try { javax.imageio.ImageIO.write(imagenNormal, "png", new java.io.File("target/procesos-ventana-normal.png")); }
                catch (java.io.IOException error) { throw new IllegalStateException(error); }
            });
            ventanaNormal[0].getControlador().configurar(new Modelo.Configuracion(new int[] {64, 96}, 700, 4, 0));
            String[][] solicitudNormal = new String[1][];
            javax.swing.SwingUtilities.invokeAndWait(() -> solicitudNormal[0] = ventanaNormal[0].getPanelProceso().leerCampos());
            Modelo.Proceso procesoNormal = Interfaces.PanelProceso.crearProceso(ventanaNormal[0].getControlador(), solicitudNormal[0]);
            if (procesoNormal.getEstado() != Modelo.EstadoProceso.LISTO) throw new AssertionError("Nuevo formulario no admite proceso");
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                Interfaces.PanelComputadores tableroNormal = ventanaNormal[0].getPanelComputadores();
                tableroNormal.mostrarVistas(ventanaNormal[0].getControlador().consultarVistas(), "Preparado", 0);
                java.awt.Container contenidoNormal = tableroNormal.getParent();
                ((javax.swing.JTabbedPane) contenidoNormal).setSelectedComponent(tableroNormal);
                ventanaNormal[0].validate();
                javax.swing.JScrollPane scrollNormal = null;
                for (java.awt.Component componenteNormal : tableroNormal.getComponents())
                    if (componenteNormal instanceof javax.swing.JScrollPane encontradoNormal) scrollNormal = encontradoNormal;
                if (scrollNormal == null || scrollNormal.getViewport().getView().getWidth() > scrollNormal.getViewport().getWidth())
                    throw new AssertionError("El tablero exige desplazamiento horizontal en ventana normal");
            });
            System.out.println("Ventana 900x680 sin maximizar: cuatro controles visibles, formulario consistente y creacion/admision desde Procesos: OK");
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
        catch (java.lang.reflect.InvocationTargetException error) { throw new IllegalStateException(error.getCause()); }
        finally {
            if (ventanaNormal[0] != null) {
                ventanaNormal[0].getControlador().close();
                javax.swing.SwingUtilities.invokeLater(() -> ventanaNormal[0].dispose());
            }
        }
        */

        /* Prueba de scroll estable verificada en consola.
        Sistema.Computador equipoScroll = new Sistema.Computador(1, 64);
        for (int procesoScroll = 0; procesoScroll < 16; procesoScroll++)
            equipoScroll.recibirProceso(new Modelo.Proceso("Proceso " + procesoScroll, 1, Modelo.TipoProceso.CPU_BOUND, 0, 1, 8, 100));
        equipoScroll.admitirProcesos();
        Interfaces.PanelComputador[] panelScroll = new Interfaces.PanelComputador[1];
        javax.swing.JFrame[] ventanaScroll = new javax.swing.JFrame[1];
        javax.swing.JScrollPane[] barraScroll = new javax.swing.JScrollPane[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                panelScroll[0] = new Interfaces.PanelComputador(); panelScroll[0].mostrar(equipoScroll.consultarVista());
                ventanaScroll[0] = new javax.swing.JFrame(); ventanaScroll[0].setContentPane(panelScroll[0]);
                ventanaScroll[0].setSize(1100, 440); ventanaScroll[0].setVisible(true); ventanaScroll[0].validate();
                javax.swing.JPanel cuerpoScroll = (javax.swing.JPanel) ((java.awt.BorderLayout) panelScroll[0].getLayout()).getLayoutComponent(java.awt.BorderLayout.CENTER);
                javax.swing.JPanel colasScroll = (javax.swing.JPanel) ((java.awt.BorderLayout) cuerpoScroll.getLayout()).getLayoutComponent(java.awt.BorderLayout.CENTER);
                javax.swing.JPanel listosScroll = (javax.swing.JPanel) colasScroll.getComponent(1);
                barraScroll[0] = (javax.swing.JScrollPane) ((java.awt.BorderLayout) listosScroll.getLayout()).getLayoutComponent(java.awt.BorderLayout.CENTER);
                if (barraScroll[0].getVerticalScrollBar().getMaximum() - barraScroll[0].getVerticalScrollBar().getVisibleAmount() < 200)
                    throw new AssertionError("La cola de prueba no tiene recorrido suficiente");
                barraScroll[0].getVerticalScrollBar().setValue(160);
                barraScroll[0].getVerticalScrollBar().setValueIsAdjusting(true);
            });
            equipoScroll.avanzarCiclo(1);
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                panelScroll[0].mostrar(equipoScroll.consultarVista()); ventanaScroll[0].validate();
                if (barraScroll[0].getParent() == null || !javax.swing.SwingUtilities.isDescendingFrom(barraScroll[0], panelScroll[0])
                        || !barraScroll[0].getVerticalScrollBar().getValueIsAdjusting()
                        || barraScroll[0].getVerticalScrollBar().getValue() != 160)
                    throw new AssertionError("Refresco interrumpe el arrastre o cambia posicion");
                barraScroll[0].getVerticalScrollBar().setValue(240);
            });
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                if (barraScroll[0].getVerticalScrollBar().getValue() != 240)
                    throw new AssertionError("Una restauracion diferida piso el arrastre");
                barraScroll[0].getVerticalScrollBar().setValueIsAdjusting(false);
                barraScroll[0].getVerticalScrollBar().setValue(barraScroll[0].getVerticalScrollBar().getMaximum());
                if (barraScroll[0].getViewport().getViewPosition().y <= 240) throw new AssertionError("No llega al final de cola");
            });
            System.out.println("Scroll: misma barra y arrastre activo tras refrescar PCB, posicion 160/240 estable y final accesible: OK");
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
        catch (java.lang.reflect.InvocationTargetException error) { throw new IllegalStateException(error.getCause()); }
        finally { if (ventanaScroll[0] != null) javax.swing.SwingUtilities.invokeLater(() -> ventanaScroll[0].dispose()); }
        */

}
}
