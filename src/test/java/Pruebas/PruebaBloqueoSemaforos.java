package Pruebas;

import Modelo.*;
import Sistema.Computador;
import Sistema.ControladorSimulacion;
import static Pruebas.PruebaSemaforos.verificar;
import static Pruebas.PruebaSemaforos.rechazar;

/** Bloqueo, despertar entre equipos y vencimientos sin reloj de pared. @author Luciano Rojas */
public final class PruebaBloqueoSemaforos {
    public static void main(String[] args) {
        comprobarFlujo();
        comprobarVencimiento();
        comprobarSenalEnTransito();
        System.out.println("OK: CPU disponible, despertar FIFO entre equipos, vencimiento y senal en transito");
    }

    private static ControladorSimulacion crearControlador() {
        ControladorSimulacion controlador = new ControladorSimulacion();
        controlador.configurar(new Configuracion(new int[] {32,32}, 700, 4, 0));
        return controlador;
    }

    private static void comprobarFlujo() {
        try (ControladorSimulacion c = crearControlador()) {
            Buffer<Object> buffer = c.crearBuffer("B", 2, 1);
            Proceso primero = c.crearProceso("Primero", 1, 0, 1, 10, 50);
            Proceso otro = c.crearProceso("Otro", 1, 0, 1, 10, 50);
            Proceso segundo = c.crearProceso("Segundo", 2, 0, 1, 10, 50);
            Computador[] equipos = c.getComputadores();
            rechazar(() -> c.esperarSemaforo(primero, buffer.getLlenos()));
            verificar(buffer.getLlenos().getEsperando().length == 0, "Encolo un proceso sin CPU");
            equipos[0].avanzarCiclo(1);
            equipos[1].avanzarCiclo(1);
            int pc = primero.getPc(), restante = primero.getRestante();
            verificar(!c.esperarSemaforo(primero, buffer.getLlenos()), "No bloqueo al primero");
            verificar(primero.getEstado() == EstadoProceso.BLOQUEADO
                    && primero.getMotivoBloqueo() == MotivoBloqueo.SEMAFORO
                    && equipos[0].getProcesoEnCPU() == null, "Estado o CPU incorrectos");
            verificar(!c.esperarSemaforo(segundo, buffer.getLlenos()), "No bloqueo al segundo");
            verificar(!equipos[0].despertarPorSemaforo(primero, buffer.getVacios()),
                    "Desperto por un semaforo distinto");
            equipos[0].avanzarCiclo(2);
            verificar(equipos[0].getProcesoEnCPU() == otro && otro.getPc() == 1,
                    "La espera detuvo el computador");
            verificar(primero.getPc() == pc && primero.getRestante() == restante,
                    "El bloqueado ejecuto instrucciones");
            verificar(c.senalizarSemaforo(buffer.getLlenos()) == primero
                    && c.senalizarSemaforo(buffer.getLlenos()) == segundo, "Despertar fuera de FIFO");
            verificar(primero.getEstado() == EstadoProceso.LISTO && segundo.getEstado() == EstadoProceso.LISTO
                    && primero.getMotivoBloqueo() == MotivoBloqueo.NINGUNO, "No regreso a listos");
            verificar(equipos[0].getProcesoEnCPU() == otro && equipos[0].getBloqueados().length == 0
                    && equipos[1].getBloqueados().length == 0 && buffer.getLlenos().getValor() == 0,
                    "El despertar cambio la CPU o duplico permisos");
            verificar(c.esperarSemaforo(otro, buffer.getVacios()) && otro.getEstado() == EstadoProceso.EJECUCION,
                    "Adquirir un permiso disponible bloqueo al proceso");
            SemaforoSimulado ajeno = new SemaforoSimulado("Ajeno", 0, 1);
            rechazar(() -> c.senalizarSemaforo(ajeno));
            c.configurar(new Configuracion(new int[] {32,32}, 700, 4, 0));
            rechazar(() -> c.senalizarSemaforo(buffer.getLlenos()));
        }
    }

    private static void comprobarVencimiento() {
        try (ControladorSimulacion c = crearControlador()) {
            Buffer<Object> buffer = c.crearBuffer("B", 2, 1);
            Proceso vence = c.crearProceso("Vence", 1, 0, 3, 10, 2);
            Computador equipo = c.getComputadores()[0];
            equipo.avanzarCiclo(1);
            c.esperarSemaforo(vence, buffer.getLlenos());
            equipo.avanzarCiclo(2);
            verificar(vence.getEstado() == EstadoProceso.TERMINADO && equipo.getBloqueados().length == 0
                    && buffer.getLlenos().getEsperando().length == 0, "Quedo una espera vencida");
            verificar(equipo.getMemoria().getUsada() == 2, "No libero la RAM del proceso");
            verificar(c.senalizarSemaforo(buffer.getLlenos()) == null && buffer.getLlenos().getValor() == 1,
                    "La espera vencida consumio un permiso");
        }
    }

    private static void comprobarSenalEnTransito() {
        try (ControladorSimulacion c = crearControlador()) {
            Buffer<Object> buffer = c.crearBuffer("B", 2, 1);
            Proceso vence = c.crearProceso("En transito", 1, 0, 1, 10, 2);
            Computador equipo = c.getComputadores()[0];
            equipo.avanzarCiclo(1);
            c.esperarSemaforo(vence, buffer.getLlenos());
            // Intercalado determinista: signal retira el receptor; vence antes de despertar.
            Proceso receptor = buffer.getLlenos().senalizar();
            equipo.avanzarCiclo(2);
            verificar(!equipo.despertarPorSemaforo(receptor, buffer.getLlenos()), "Revive un proceso terminado");
            c.senalizarSemaforo(buffer.getLlenos());
            verificar(buffer.getLlenos().getValor() == 1, "Se perdio el permiso en transito");
        }
    }
}
