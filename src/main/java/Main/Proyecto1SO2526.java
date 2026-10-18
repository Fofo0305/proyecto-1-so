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
