/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package Main;

/**
 *
 * @author Adolfo
 */
public class Proyecto1SO2526 {

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new Interfaces.VentanaPrincipal().setVisible(true));

        /* Prueba de configuracion de ejemplo verificada en consola.
        try (Sistema.ControladorSimulacion ejemploControl = new Sistema.ControladorSimulacion()) {
            Modelo.CargaInicial cargaEjemplo = Ejemplos.ConfiguracionInicial.crear();
            Modelo.CargaInicial csvEjemplo = Persistencia.ArchivoConfiguracion.cargar(java.nio.file.Path.of("ejemplos", "cluster-inicial.csv"));
            if (!java.util.Arrays.equals(cargaEjemplo.getProcesos(), csvEjemplo.getProcesos())
                    || !java.util.Arrays.equals(cargaEjemplo.getBuffers(), csvEjemplo.getBuffers())
                    || !java.util.Arrays.equals(cargaEjemplo.getConfiguracion().getRam(), csvEjemplo.getConfiguracion().getRam())
                    || cargaEjemplo.getConfiguracion().getDuracionMs() != csvEjemplo.getConfiguracion().getDuracionMs())
                throw new AssertionError("Ejemplo Java y CSV diferentes");
            Interfaces.PanelArchivos[] ejemploPanel = new Interfaces.PanelArchivos[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ejemploPanel[0] = new Interfaces.PanelArchivos(ejemploControl, carga -> {
                    if (!javax.swing.SwingUtilities.isEventDispatchThread()) throw new AssertionError("Callback fuera EDT");
                });
                ejemploPanel[0].cargarEjemplo();
            });
            String[] estadoEjemplo = {""};
            long limiteEjemplo = System.nanoTime() + 2_000_000_000L;
            do {
                javax.swing.SwingUtilities.invokeAndWait(() -> estadoEjemplo[0] = ejemploPanel[0].getResultado());
                if (estadoEjemplo[0].startsWith("Ejemplo listo")) break;
                Thread.sleep(5);
            } while (System.nanoTime() < limiteEjemplo);
            if (!estadoEjemplo[0].startsWith("Ejemplo listo")) throw new AssertionError("No cargo ejemplo: " + estadoEjemplo[0]);
            Sistema.Computador[] equiposEjemplo = ejemploControl.getComputadores();
            if (equiposEjemplo[0].getNuevos().length != 1 || equiposEjemplo[0].getListos().length != 2)
                throw new AssertionError("Ejemplo no demuestra espera por RAM");
            for (int cicloEjemplo = 1; cicloEjemplo <= 20; cicloEjemplo++)
                for (Sistema.Computador equipoEjemplo : equiposEjemplo) equipoEjemplo.avanzarCiclo(cicloEjemplo);
            if (equiposEjemplo[0].getTerminados().length != 3 || equiposEjemplo[1].getTerminados().length != 2
                    || equiposEjemplo[0].getMemoria().getUsada() != 8 || equiposEjemplo[1].getMemoria().getUsada() != 0)
                throw new AssertionError("Ejemplo no termina o pierde reserva del buffer");
            System.out.println("Ejemplo Java/CSV y boton: dos computadores, cinco procesos, espera RAM, readmision y buffer de 8 unidades: OK");
        } catch (java.io.IOException | java.lang.reflect.InvocationTargetException error) { throw new IllegalStateException(error); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
        */
        /* Prueba de colas graficas verificada en consola.
        Sistema.Computador graficoUno = new Sistema.Computador(1, 64);
        Sistema.Computador graficoDos = new Sistema.Computador(2, 64);
        graficoUno.recibirProceso(new Modelo.Proceso("Compilar", 1, Modelo.TipoProceso.CPU_BOUND, 1, 32, 8, 30));
        graficoUno.recibirProceso(new Modelo.Proceso("Exportar", 1, Modelo.TipoProceso.CPU_BOUND, 2, 32, 4, 30));
        graficoUno.recibirProceso(new Modelo.Proceso("Esperando memoria", 1, Modelo.TipoProceso.CPU_BOUND, 0, 32, 3, 30));
        graficoDos.recibirProceso(new Modelo.Proceso("Calculo", 2, Modelo.TipoProceso.CPU_BOUND, 0, 16, 1, 30));
        graficoDos.recibirProceso(new Modelo.Proceso("Reporte", 2, Modelo.TipoProceso.CPU_BOUND, 1, 32, 6, 30));
        graficoUno.avanzarCiclo(1); graficoDos.avanzarCiclo(1);
        Modelo.VistaComputador[] graficas = {graficoUno.consultarVista(), graficoDos.consultarVista()};
        if (graficas[0].procesoCPU() == null || graficas[0].colaNuevos().length != 1
                || graficas[0].colaListos().length != 1 || graficas[1].colaTerminados().length != 1)
            throw new AssertionError("No se capturaron las distintas colas");
        Modelo.VistaProceso[] copiaGrafica = graficas[0].colaListos(); copiaGrafica[0] = null;
        if (graficas[0].colaListos()[0] == null) throw new AssertionError("Vista expone arreglo mutable");
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                Interfaces.PanelComputadores panelGrafico = new Interfaces.PanelComputadores();
                panelGrafico.mostrarVistas(graficas, "Pausado · inspeccion del ciclo", 1);
                if (panelGrafico.getComputadoresVisibles() != 2) throw new AssertionError("No muestra el cluster");
                panelGrafico.seleccionarComputador(1);
                if (panelGrafico.getComputadoresVisibles() != 1) throw new AssertionError("Filtro no enfoca computador");
                panelGrafico.mostrarDetallesPCB(true);
                panelGrafico.mostrarVistas(graficas, "Pausado", 1);
                if (panelGrafico.getComputadoresVisibles() != 1) throw new AssertionError("Refresco pierde filtro");
                panelGrafico.seleccionarComputador(0);
                panelGrafico.mostrarDetallesPCB(false);
                if (panelGrafico.getComputadoresVisibles() != 2) throw new AssertionError("No restauro vista general");
                javax.swing.JFrame ventanaGrafica = new javax.swing.JFrame("Simulacion grafica");
                try {
                    ventanaGrafica.setContentPane(panelGrafico); ventanaGrafica.setSize(1200, 1000); ventanaGrafica.setVisible(true);
                    ventanaGrafica.validate();
                    if (!panelGrafico.getDetalle().contains("Esperando memoria") || !panelGrafico.getDetalle().contains("Calculo"))
                        throw new AssertionError("Vista pierde procesos");
                    java.awt.image.BufferedImage imagenGrafica = new java.awt.image.BufferedImage(1200, 1000, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D pincelGrafico = imagenGrafica.createGraphics();
                    try { ventanaGrafica.printAll(pincelGrafico); } finally { pincelGrafico.dispose(); }
                    try { javax.imageio.ImageIO.write(imagenGrafica, "png", new java.io.File("target/colas-graficas.png")); }
                    catch (java.io.IOException error) { throw new IllegalStateException(error); }
                } finally { ventanaGrafica.dispose(); }
            });
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
        catch (java.lang.reflect.InvocationTargetException error) { throw new IllegalStateException(error.getCause()); }
        System.out.println("Colas graficas: CPU, FIFO, RAM, filtro persistente, detalles PCB opcionales y copias aisladas: OK");
        */
        /* Prueba de configuracion rechazada verificada en consola.
        Interfaces.VentanaPrincipal[] rechazoVentana = new Interfaces.VentanaPrincipal[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> rechazoVentana[0] = new Interfaces.VentanaPrincipal());
            rechazoVentana[0].getControlador().configurar(new Modelo.Configuracion(new int[] {32, 64}, 100, 4, 0));
            rechazoVentana[0].getControlador().iniciar();
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                rechazoVentana[0].getPanelConfiguracion().mostrarConfiguracion(new Modelo.Configuracion(new int[] {64, 64}, 777, 4, 0));
                rechazoVentana[0].aplicarConfiguracion();
            });
            String[] rechazoEstado = {""};
            long rechazoLimite = System.nanoTime() + 2_000_000_000L;
            do {
                javax.swing.SwingUtilities.invokeAndWait(() -> rechazoEstado[0] = rechazoVentana[0].getEstado());
                if (!rechazoEstado[0].equals("Aplicando configuracion...")) break;
                Thread.sleep(5);
            } while (System.nanoTime() < rechazoLimite);
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                if (!rechazoVentana[0].getEstado().contains("Detenga")
                        || !"100".equals(rechazoVentana[0].getPanelControles().getDuracionMostrada()))
                    throw new AssertionError("Cambio rechazado altero controles o no mostro error");
            });
            if (rechazoVentana[0].getControlador().getConfiguracion().getDuracionMs() != 100)
                throw new AssertionError("Cambio rechazado altero sesion");
            System.out.println("Configuracion rechazada con reloj activo: error visible, sesion y ciclo mostrado conservados en 100 ms: OK");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        } catch (java.lang.reflect.InvocationTargetException error) {
            throw new IllegalStateException(error.getCause());
        } finally {
            if (rechazoVentana[0] != null) {
                rechazoVentana[0].getControlador().close();
                javax.swing.SwingUtilities.invokeLater(() -> rechazoVentana[0].dispose());
            }
        }
        */
        /* Prueba integrada de persistencia verificada en consola.
        try (Sistema.ControladorSimulacion persistenciaFinal = new Sistema.ControladorSimulacion()) {
            persistenciaFinal.configurar(new Modelo.Configuracion(new int[] {32, 64}, 5, 3, 2));
            persistenciaFinal.crearProceso("Primero, con comilla \"A\"", 1, 0, 32, 2, 20);
            persistenciaFinal.crearProceso("Espera", 1, 1, 32, 1, 20);
            java.nio.file.Path rutaFinal = java.nio.file.Path.of("target", "integracion-final.csv");
            Persistencia.ArchivoConfiguracion.guardar(rutaFinal, persistenciaFinal.getCargaInicial());
            persistenciaFinal.iniciar();
            long limiteFinal = System.nanoTime() + 2_000_000_000L;
            while (persistenciaFinal.getCiclo() < 3 && System.nanoTime() < limiteFinal) Thread.sleep(5);
            persistenciaFinal.detener();
            if (persistenciaFinal.getComputadores()[0].getTerminados().length != 2)
                throw new AssertionError("No completo flujo antes de restaurar");
            persistenciaFinal.cargarCarga(Persistencia.ArchivoConfiguracion.cargar(rutaFinal));
            if (persistenciaFinal.getCiclo() != 0 || persistenciaFinal.getComputadores()[0].getListos().length != 1
                    || persistenciaFinal.getComputadores()[0].getNuevos().length != 1
                    || persistenciaFinal.getCargaInicial().getProcesos()[0].deadline() != 20)
                throw new AssertionError("Restauracion no reconstruye la carga original");
            Sistema.Computador[] sesionAntesError = persistenciaFinal.getComputadores();
            java.nio.file.Path rutaFinalInvalida = java.nio.file.Path.of("target", "integracion-invalida.csv");
            java.nio.file.Files.writeString(rutaFinalInvalida, Persistencia.Csv.registro("AVILAOS", "1")
                    + Persistencia.Csv.registro("CONFIG", "5", "3", "2")
                    + Persistencia.Csv.registro("COMPUTADOR", "1", "32", "FCFS", "3")
                    + Persistencia.Csv.registro("COMPUTADOR", "2", "64", "FCFS", "3")
                    + Persistencia.Csv.registro("PROCESO", "Invalido", "3", "0", "1", "1", "1", "CPU_BOUND"));
            try {
                persistenciaFinal.cargarCarga(Persistencia.ArchivoConfiguracion.cargar(rutaFinalInvalida));
                throw new AssertionError("Archivo invalido reemplazo sesion");
            } catch (IllegalArgumentException esperado) {
                if (persistenciaFinal.getComputadores()[0] != sesionAntesError[0])
                    throw new AssertionError("Fallo de carga altero sesion existente");
            }
            System.out.println("Persistencia integrada: ejecutar, guardar carga original, reconstruir PC/ciclo cero y conservar sesion ante archivo invalido: OK");
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba final interrumpida", interrupcion);
        } catch (java.io.IOException errorFinal) {
            throw new IllegalStateException("Fallo de prueba final", errorFinal);
        }
        */

        /* Prueba de archivos desde interfaz verificada en consola.
        try (Sistema.ControladorSimulacion archivoPanelControl = new Sistema.ControladorSimulacion()) {
            archivoPanelControl.configurar(new Modelo.Configuracion(new int[] {64, 128}, 100, 4, 0));
            archivoPanelControl.crearProceso("Persistido", 2, 1, 32, 3, 20);
            java.nio.file.Path archivoPanelRuta = java.nio.file.Path.of("target", "panel-archivo.csv");
            Interfaces.PanelArchivos[] archivoPanel = new Interfaces.PanelArchivos[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                archivoPanel[0] = new Interfaces.PanelArchivos(archivoPanelControl, carga -> {
                    if (!javax.swing.SwingUtilities.isEventDispatchThread()) throw new AssertionError("Callback fuera EDT");
                });
                archivoPanel[0].procesarArchivo(archivoPanelRuta, true);
            });
            String[] resultadoArchivoPanel = {""};
            long limiteArchivoPanel = System.nanoTime() + 2_000_000_000L;
            do {
                javax.swing.SwingUtilities.invokeAndWait(() -> resultadoArchivoPanel[0] = archivoPanel[0].getResultado());
                if (resultadoArchivoPanel[0].startsWith("Guardado:")) break;
                Thread.sleep(5);
            } while (System.nanoTime() < limiteArchivoPanel);
            if (!resultadoArchivoPanel[0].startsWith("Guardado:")) throw new AssertionError("No guardo desde panel: " + resultadoArchivoPanel[0]);
            archivoPanelControl.configurar(new Modelo.Configuracion(new int[] {16, 16}, 10, 2, 0));
            javax.swing.SwingUtilities.invokeAndWait(() -> archivoPanel[0].procesarArchivo(archivoPanelRuta, false));
            limiteArchivoPanel = System.nanoTime() + 2_000_000_000L;
            do {
                javax.swing.SwingUtilities.invokeAndWait(() -> resultadoArchivoPanel[0] = archivoPanel[0].getResultado());
                if (resultadoArchivoPanel[0].startsWith("Cargado:")) break;
                Thread.sleep(5);
            } while (System.nanoTime() < limiteArchivoPanel);
            if (!resultadoArchivoPanel[0].startsWith("Cargado:") || archivoPanelControl.getConfiguracion().getRam()[1] != 128
                    || !"Persistido".equals(archivoPanelControl.getCargaInicial().getProcesos()[0].nombre()))
                throw new AssertionError("Carga desde panel incorrecta");
            System.out.println("Panel de archivos: guardar/cargar en trabajador, callback EDT y carga reconstruida: OK");
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba de archivos interrumpida", interrupcion);
        } catch (java.lang.reflect.InvocationTargetException errorPanelArchivo) {
            throw new IllegalStateException("Error de panel de archivos", errorPanelArchivo.getCause());
        }
        System.out.println("Prueba de persistencia desde interfaz: OK");
        */

        /* Prueba integrada de interfaz verificada en consola.
        Interfaces.VentanaPrincipal[] interfazIntegrada = new Interfaces.VentanaPrincipal[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                interfazIntegrada[0] = new Interfaces.VentanaPrincipal();
                interfazIntegrada[0].setVisible(true);
                interfazIntegrada[0].getPanelConfiguracion().mostrarConfiguracion(new Modelo.Configuracion(new int[] {32, 64}, 10, 4, 0));
                interfazIntegrada[0].aplicarConfiguracion();
            });
            Sistema.ControladorSimulacion interfazControlador = interfazIntegrada[0].getControlador();
            long limiteInterfaz = System.nanoTime() + 2_000_000_000L;
            while (interfazControlador.getConfiguracion() == null && System.nanoTime() < limiteInterfaz) Thread.sleep(5);
            Modelo.Proceso interfazA = interfazControlador.crearProceso("Primero", 1, 0, 32, 2, 20);
            Modelo.Proceso interfazB = interfazControlador.crearProceso("Espera RAM", 1, 1, 32, 2, 20);
            interfazControlador.crearProceso("Otro computador", 2, 0, 32, 3, 20);
            if (interfazB.getEstado() != Modelo.EstadoProceso.NUEVO) throw new AssertionError("No hubo espera por RAM");
            javax.swing.SwingUtilities.invokeAndWait(() -> interfazIntegrada[0].getPanelControles().ejecutarAccion("iniciar"));
            limiteInterfaz = System.nanoTime() + 2_000_000_000L;
            while (interfazControlador.getCiclo() < 4 && System.nanoTime() < limiteInterfaz) Thread.sleep(5);
            interfazControlador.pausar();
            Modelo.VistaComputador[] vistaInterfaz = interfazControlador.consultarVistas();
            if (interfazA.getEstado() != Modelo.EstadoProceso.TERMINADO || interfazB.getEstado() != Modelo.EstadoProceso.TERMINADO
                    || vistaInterfaz[0].usada() != 0 || vistaInterfaz[1].usada() != 0)
                throw new AssertionError("Flujo grafico integrado no completo procesos");
            long cicloInterfaz = interfazControlador.getCiclo();
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                interfazIntegrada[0].getPanelComputadores().mostrarVistas(vistaInterfaz, "Pausado", cicloInterfaz);
                if (!interfazIntegrada[0].getPanelComputadores().getDetalle().contains("Espera RAM"))
                    throw new AssertionError("Vista no refleja proceso readmitido");
                java.awt.image.BufferedImage imagenInterfaz = new java.awt.image.BufferedImage(interfazIntegrada[0].getWidth(), interfazIntegrada[0].getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D pincelInterfaz = imagenInterfaz.createGraphics();
                try { interfazIntegrada[0].printAll(pincelInterfaz); }
                finally { pincelInterfaz.dispose(); }
                try { javax.imageio.ImageIO.write(imagenInterfaz, "png", new java.io.File("target/interfaz-verificada.png")); }
                catch (java.io.IOException errorImagen) { throw new IllegalStateException(errorImagen); }
            });
            System.out.println("Interfaz integrada: configuracion, espera RAM, FCFS en dos computadores y vista final: OK");
            System.out.println("Representacion de ventana guardada en target/interfaz-verificada.png");
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba integrada interrumpida", interrupcion);
        } catch (java.lang.reflect.InvocationTargetException errorIntegracion) {
            throw new IllegalStateException("Fallo de interfaz integrada", errorIntegracion.getCause());
        } finally {
            if (interfazIntegrada[0] != null) {
                interfazIntegrada[0].getControlador().close();
                javax.swing.SwingUtilities.invokeLater(interfazIntegrada[0]::dispose);
            }
        }

        */

}
}
