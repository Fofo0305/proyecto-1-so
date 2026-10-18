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

        /* Prueba de controles del controlador verificada en consola.
        try (Sistema.ControladorSimulacion controlesPrueba = new Sistema.ControladorSimulacion()) {
            controlesPrueba.configurar(new Modelo.Configuracion(new int[] {32, 32}, 5, 4, 0));
            controlesPrueba.crearProceso("Control", 1, 0, 16, 2, 10);
            controlesPrueba.iniciar();
            long limiteControles = System.nanoTime() + 2_000_000_000L;
            while (controlesPrueba.getCiclo() < 2 && System.nanoTime() < limiteControles) Thread.sleep(2);
            controlesPrueba.pausar();
            long cicloPausaControl = controlesPrueba.getCiclo();
            Thread.sleep(20);
            if (cicloPausaControl < 2 || controlesPrueba.getCiclo() != cicloPausaControl || !"Pausado".equals(controlesPrueba.getEstado()))
                throw new AssertionError("Pausa desde controlador incorrecta");
            try {
                controlesPrueba.configurar(new Modelo.Configuracion(new int[] {16, 16}, 10, 4, 0));
                throw new AssertionError("Se reemplazo simulacion activa");
            } catch (IllegalStateException esperado) { }
            controlesPrueba.cambiarDuracion(2);
            controlesPrueba.reanudar();
            limiteControles = System.nanoTime() + 2_000_000_000L;
            while (controlesPrueba.getCiclo() == cicloPausaControl && System.nanoTime() < limiteControles) Thread.sleep(2);
            controlesPrueba.detener();
            if (controlesPrueba.getCiclo() <= cicloPausaControl || !"Detenido".equals(controlesPrueba.getEstado())
                    || controlesPrueba.getConfiguracion().getDuracionMs() != 2) throw new AssertionError("Reanudacion o cierre incorrectos");
            controlesPrueba.configurar(new Modelo.Configuracion(new int[] {64, 64}, 100, 2, 0));
            if (!"Preparado".equals(controlesPrueba.getEstado()) || controlesPrueba.getCiclo() != 0)
                throw new AssertionError("No se pudo crear una nueva sesion");
            System.out.println("Controlador: inicio, pausa, reanudacion, duracion y detencion; nueva sesion en ciclo cero: OK");
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba de controles interrumpida", interrupcion);
        }
        System.out.println("Prueba de controles de simulacion fuera de EDT: OK");
        */

        /* Prueba de formulario de procesos verificada en consola.
        Interfaces.PanelProceso[] panelProcesoPrueba = new Interfaces.PanelProceso[1];
        try (Sistema.ControladorSimulacion controladorPanelProceso = new Sistema.ControladorSimulacion()) {
            controladorPanelProceso.configurar(new Modelo.Configuracion(new int[] {32, 64}, 100, 4, 0));
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> panelProcesoPrueba[0] = new Interfaces.PanelProceso());
                String[][] camposPanel = new String[1][];
                javax.swing.SwingUtilities.invokeAndWait(() -> camposPanel[0] = panelProcesoPrueba[0].leerCampos());
                Modelo.Proceso procesoDesdePanel = Interfaces.PanelProceso.crearProceso(controladorPanelProceso, camposPanel[0]);
                Modelo.Proceso esperaDesdePanel = Interfaces.PanelProceso.crearProceso(controladorPanelProceso, camposPanel[0]);
                if (procesoDesdePanel.getEstado() != Modelo.EstadoProceso.LISTO || esperaDesdePanel.getEstado() != Modelo.EstadoProceso.NUEVO)
                    throw new AssertionError("Formulario no respeta admision");
                camposPanel[0][1] = "texto";
                try {
                    Interfaces.PanelProceso.crearProceso(controladorPanelProceso, camposPanel[0]);
                    throw new AssertionError("Texto numerico invalido aceptado");
                } catch (IllegalArgumentException esperado) { }
                if (controladorPanelProceso.getComputadores()[0].getNuevos().length != 1)
                    throw new AssertionError("Entrada invalida altero cola");
            } catch (InterruptedException interrupcion) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Prueba de formulario interrumpida", interrupcion);
            } catch (java.lang.reflect.InvocationTargetException falloPanel) {
                throw new IllegalStateException("Fallo de formulario", falloPanel.getCause());
            }
            System.out.println("Formulario de procesos: captura EDT, creacion fuera EDT, listo y espera RAM, texto invalido rechazado: OK");
        }
        System.out.println("Prueba de PanelProceso desde main: OK");
        */

        /* Prueba de creacion manual de procesos verificada en consola.
        try (Sistema.ControladorSimulacion creacionPrueba = new Sistema.ControladorSimulacion()) {
            try {
                creacionPrueba.crearProceso("Sin cluster", 1, 0, 1, 1, 1);
                throw new AssertionError("Proceso creado sin cluster");
            } catch (IllegalStateException esperado) { }
            creacionPrueba.configurar(new Modelo.Configuracion(new int[] {64, 128}, 100, 4, 0));
            Modelo.Proceso creadoPrimero = creacionPrueba.crearProceso("Primero", 1, 0, 64, 2, 10);
            Modelo.Proceso creadoEspera = creacionPrueba.crearProceso("Espera", 1, 0, 32, 2, 10);
            Modelo.Proceso creadoOtro = creacionPrueba.crearProceso("Otro", 2, 0, 128, 2, 10);
            if (creadoPrimero.getEstado() != Modelo.EstadoProceso.LISTO || creadoEspera.getEstado() != Modelo.EstadoProceso.NUEVO
                    || creadoOtro.getEstado() != Modelo.EstadoProceso.LISTO || creadoOtro.getComputador() != 2)
                throw new AssertionError("Asignacion o admision desde controlador incorrecta");
            for (int casoCreacion = 0; casoCreacion < 3; casoCreacion++) {
                try {
                    creacionPrueba.crearProceso(casoCreacion == 2 ? "" : "Invalido", casoCreacion == 0 ? 3 : 1,
                            0, casoCreacion == 1 ? 65 : 1, 1, 1);
                    throw new AssertionError("Solicitud de proceso invalida aceptada");
                } catch (IllegalArgumentException esperado) { }
            }
            Sistema.Computador[] destinoCreacion = creacionPrueba.getComputadores();
            if (destinoCreacion[0].getListos().length != 1 || destinoCreacion[0].getNuevos().length != 1
                    || destinoCreacion[1].getListos().length != 1)
                throw new AssertionError("Entradas invalidas alteraron colas");
            System.out.println("Creacion manual: listo, nuevo por falta de RAM y otro computador listo: OK");
        }
        System.out.println("Prueba de creacion de procesos desde controlador: OK");
        */

        /* Prueba de configuracion desde ventana verificada en consola.
        Interfaces.VentanaPrincipal[] ventanaConfiguracion = new Interfaces.VentanaPrincipal[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ventanaConfiguracion[0] = new Interfaces.VentanaPrincipal();
                ventanaConfiguracion[0].setVisible(true);
                ventanaConfiguracion[0].getPanelConfiguracion().mostrarConfiguracion(new Modelo.Configuracion(new int[] {64, 96}, 10, 2, 0));
                ventanaConfiguracion[0].aplicarConfiguracion();
            });
            long limiteVentana = System.nanoTime() + 2_000_000_000L;
            while (ventanaConfiguracion[0].getControlador().getConfiguracion() == null && System.nanoTime() < limiteVentana) Thread.sleep(5);
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                if (ventanaConfiguracion[0].getControlador().getComputadores().length != 2
                        || ventanaConfiguracion[0].getControlador().getComputadores()[1].getMemoria().getCapacidad() != 96)
                    throw new AssertionError("La ventana no configuro el controlador");
                javax.swing.JTextField campoInvalidoVentana = (javax.swing.JTextField) ventanaConfiguracion[0].getPanelConfiguracion().getComponent(1);
                campoInvalidoVentana.setText("no-numero");
                ventanaConfiguracion[0].aplicarConfiguracion();
                if (!ventanaConfiguracion[0].getEstado().contains("enteros")) throw new AssertionError("Error no mostrado");
                System.out.println("Ventana: cluster construido fuera de EDT y error de entrada visible: OK");
            });
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba de configuracion interrumpida", interrupcion);
        } catch (java.lang.reflect.InvocationTargetException errorVentana) {
            throw new IllegalStateException("Fallo de configuracion grafica", errorVentana.getCause());
        } finally {
            if (ventanaConfiguracion[0] != null) {
                ventanaConfiguracion[0].getControlador().close();
                javax.swing.SwingUtilities.invokeLater(ventanaConfiguracion[0]::dispose);
            }
        }
        System.out.println("Prueba de configuracion desde ventana principal: OK");
        */

        /* Prueba de panel configuracion verificada en consola.
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                Interfaces.PanelConfiguracion panelConfiguracionPrueba = new Interfaces.PanelConfiguracion();
                if (panelConfiguracionPrueba.leerConfiguracion().getCantidadComputadores() != 2)
                    throw new AssertionError("Cantidad inicial incorrecta");
                panelConfiguracionPrueba.mostrarConfiguracion(new Modelo.Configuracion(new int[] {32, 64, 96}, 50, 2, 1));
                Modelo.Configuracion lecturaPanel = panelConfiguracionPrueba.leerConfiguracion();
                if (lecturaPanel.getCantidadComputadores() != 3 || lecturaPanel.getRam()[2] != 96
                        || lecturaPanel.getDuracionMs() != 50 || lecturaPanel.getQuantum() != 2 || lecturaPanel.getLatencia() != 1)
                    throw new AssertionError("El panel no conserva parametros");
                javax.swing.JTextField campoCantidadPrueba = (javax.swing.JTextField) panelConfiguracionPrueba.getComponent(1);
                campoCantidadPrueba.setText("texto");
                try {
                    panelConfiguracionPrueba.leerConfiguracion();
                    throw new AssertionError("Texto invalido aceptado");
                } catch (IllegalArgumentException esperado) { }
                campoCantidadPrueba.setText("2");
                try {
                    panelConfiguracionPrueba.leerConfiguracion();
                    throw new AssertionError("Cantidad no coincide con RAM");
                } catch (IllegalArgumentException esperado) { }
                System.out.println("Panel de configuracion: carga/lectura de tres computadores, numeros invalidos y RAM incoherente: OK");
            });
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba de panel interrumpida", interrupcion);
        } catch (java.lang.reflect.InvocationTargetException errorPanel) {
            throw new IllegalStateException("Fallo de panel", errorPanel.getCause());
        }
        System.out.println("Prueba de PanelConfiguracion en EDT: OK");
        */

        /* Prueba de controlador y ensamblado verificada en consola.
        try (Sistema.ControladorSimulacion controladorPrueba = new Sistema.ControladorSimulacion()) {
            if (controladorPrueba.getComputadores().length != 0) throw new AssertionError("Cluster prematuro");
            Modelo.Configuracion inicialControlador = new Modelo.Configuracion(new int[] {64, 128}, 20, 3, 0);
            controladorPrueba.configurar(inicialControlador);
            Sistema.Computador[] computadoresPrueba = controladorPrueba.getComputadores();
            if (computadoresPrueba.length != 2 || computadoresPrueba[0].getId() != 1 || computadoresPrueba[1].getId() != 2
                    || computadoresPrueba[0].getMemoria().getCapacidad() != 64 || computadoresPrueba[1].getMemoria().getCapacidad() != 128
                    || controladorPrueba.getCiclo() != 0 || controladorPrueba.getConfiguracion() != inicialControlador)
                throw new AssertionError("Ensamblado de cluster incorrecto");
            computadoresPrueba[0] = null;
            if (controladorPrueba.getComputadores()[0] == null) throw new AssertionError("Arreglo interno expuesto");
            try {
                controladorPrueba.configurar(null);
                throw new AssertionError("Configuracion nula aceptada");
            } catch (IllegalArgumentException esperado) {
                if (controladorPrueba.getConfiguracion() != inicialControlador) throw new AssertionError("Se perdio configuracion anterior");
            }
            controladorPrueba.configurar(new Modelo.Configuracion(new int[] {32, 64, 96}, 10, 2, 1));
            if (controladorPrueba.getComputadores().length != 3) throw new AssertionError("No reemplazo cluster");
            System.out.println("Controlador: cluster de dos computadores y reemplazo por tres con RAM independiente: OK");
        }
        System.out.println("Prueba de ensamblado y cierre del controlador: OK");
        */

        /* Prueba de configuracion del cluster verificada en consola.
        int[] ramConfiguracion = {64, 128};
        Modelo.Configuracion configuracionPrueba = new Modelo.Configuracion(ramConfiguracion, 100, 4, 2);
        ramConfiguracion[0] = 1;
        int[] copiaConfiguracion = configuracionPrueba.getRam();
        copiaConfiguracion[1] = 1;
        if (configuracionPrueba.getRam()[0] != 64 || configuracionPrueba.getRam()[1] != 128
                || configuracionPrueba.getCantidadComputadores() != 2 || configuracionPrueba.getDuracionMs() != 100
                || configuracionPrueba.getQuantum() != 4 || configuracionPrueba.getLatencia() != 2
                || configuracionPrueba.getPoliticaInicial() != Modelo.PoliticaPlanificacion.FCFS)
            throw new AssertionError("Configuracion o copia de RAM incorrecta");
        int rechazadosConfiguracion = 0;
        for (int casoConfiguracion = 0; casoConfiguracion < 7; casoConfiguracion++) {
            try {
                new Modelo.Configuracion(casoConfiguracion == 0 ? null : casoConfiguracion == 1 ? new int[] {64}
                        : casoConfiguracion == 2 ? new int[] {64, 0} : new int[] {64, 128},
                        casoConfiguracion == 3 ? 0 : casoConfiguracion == 4 ? 60001 : 100,
                        casoConfiguracion == 5 ? 0 : 4, casoConfiguracion == 6 ? -1 : 2);
                throw new AssertionError("Configuracion invalida aceptada");
            } catch (IllegalArgumentException esperado) { rechazadosConfiguracion++; }
        }
        System.out.println("Configuracion: 2 computadores, RAM 64/128, ciclo 100, quantum 4, latencia 2; copias aisladas: OK");
        System.out.println("Configuraciones invalidas rechazadas: " + rechazadosConfiguracion + " (esperado: 7)");
        System.out.println("Prueba de configuracion del cluster: OK");
        */

        /* Prueba de ventana principal verificada en consola.
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                Interfaces.VentanaPrincipal ventanaPrueba = new Interfaces.VentanaPrincipal();
                try {
                    ventanaPrueba.setVisible(true);
                    System.out.println("Ventana visible: " + ventanaPrueba.isShowing() + " (esperado: true)");
                    System.out.println("Titulo: " + ventanaPrueba.getTitle());
                    if (!ventanaPrueba.isShowing() || !"AvilaOS - Simulador distribuido".equals(ventanaPrueba.getTitle())
                            || ventanaPrueba.getContentPane().getComponentCount() != 1)
                        throw new AssertionError("Ventana inicial incorrecta");
                } finally { ventanaPrueba.dispose(); }
                System.out.println("Ventana liberada: " + !ventanaPrueba.isDisplayable() + " (esperado: true)");
            });
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba de ventana interrumpida", interrupcion);
        } catch (java.lang.reflect.InvocationTargetException errorVentana) {
            throw new IllegalStateException("Fallo de ventana", errorVentana.getCause());
        }
        System.out.println("Prueba de ventana principal desde main: OK");
        */

/* Prueba integrada de admision FCFS y reloj verificada en consola.
        Sistema.Computador globalUno = new Sistema.Computador(1, 32);
        Sistema.Computador globalDos = new Sistema.Computador(2, 32);
        Modelo.Proceso globalA = new Modelo.Proceso("A", 1, Modelo.TipoProceso.CPU_BOUND, 0, 32, 2, 2);
        Modelo.Proceso globalB = new Modelo.Proceso("B", 1, Modelo.TipoProceso.CPU_BOUND, 0, 32, 1, 5);
        Modelo.Proceso globalC = new Modelo.Proceso("C", 2, Modelo.TipoProceso.CPU_BOUND, 0, 16, 3, 5);
        Modelo.Proceso globalVence = new Modelo.Proceso("Vence", 2, Modelo.TipoProceso.CPU_BOUND, 0, 16, 5, 1);
        globalUno.recibirProceso(globalA);
        globalUno.recibirProceso(globalB);
        globalDos.recibirProceso(globalC);
        globalDos.recibirProceso(globalVence);
        try (Sistema.RelojGlobal globalIntegrado = new Sistema.RelojGlobal(new Sistema.ComponenteReloj[] {globalUno, globalDos})) {
            for (int cicloGlobal = 1; cicloGlobal <= 3; cicloGlobal++) {
                globalIntegrado.avanzarCiclo();
                if (globalUno.getUltimoCiclo() != cicloGlobal || globalDos.getUltimoCiclo() != cicloGlobal)
                    throw new AssertionError("Salto o repeticion de ciclo global");
                System.out.println("Ciclo integrado " + cicloGlobal + ": PC A/B/C=" + globalA.getPc() + "/" + globalB.getPc() + "/" + globalC.getPc());
            }
            if (globalA.getPc() != 2 || globalA.getRestante() != 0 || globalA.getDeadline() != 0
                    || globalB.getPc() != 1 || globalC.getPc() != 3 || globalVence.getPc() != 0
                    || globalUno.getTerminados().length != 2 || globalDos.getTerminados().length != 2
                    || globalUno.getMemoria().getUsada() != 0 || globalDos.getMemoria().getUsada() != 0)
                throw new AssertionError("Integracion de CPU, plazos y RAM incorrecta");
        }
        try (Sistema.RelojGlobal globalError = new Sistema.RelojGlobal(new Sistema.ComponenteReloj[] {
                cicloError -> { throw new IllegalStateException("fallo controlado automatico"); }, cicloSinFallo -> { }})) {
            globalError.setDuracionMs(1);
            globalError.iniciar();
            long limiteGlobalError = System.nanoTime() + 2_000_000_000L;
            while (globalError.estaActivo() && System.nanoTime() < limiteGlobalError) Thread.sleep(2);
            if (globalError.estaActivo() || globalError.getError() == null || globalError.getError().getCause() == null)
                throw new AssertionError("Fallo automatico no reportado o trabajadores sin cerrar");
            System.out.println("Fallo automatico propagado y motor cerrado: OK");
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba integrada interrumpida", interrupcion);
        }
        System.out.println("Prueba integrada de admision, FCFS y reloj global: OK");
        */

}
}
