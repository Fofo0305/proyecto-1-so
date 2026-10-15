package com.mycompany.proyecto1so2526;

import com.mycompany.proyecto1so2526.estructuras.Cola;
import com.mycompany.proyecto1so2526.modelo.*;

/** Comprobaciones sin librerías externas. Ejecutar main con Java 22.
 * @author Adolfo
 */
public final class PruebaModelo {
    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    public static void main(String[] args) throws InterruptedException {
        Cola<Proceso> cola = new Cola<>();
        Thread[] productores = new Thread[4];
        for (int i = 0; i < productores.length; i++) {
            productores[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++)
                    cola.encolar(new Proceso("P", 1, TipoProceso.CPU_BOUND, 0, 16, 1, 2));
            });
            productores[i].start();
        }
        for (Thread hilo : productores) hilo.join();
        verificar(cola.getCantidad() == 400, "Se perdieron procesos concurrentes");
        Object[] procesos = cola.instantanea();
        for (int i = 0; i < procesos.length; i++)
            for (int j = i + 1; j < procesos.length; j++)
                verificar(((Proceso) procesos[i]).getId() != ((Proceso) procesos[j]).getId(), "ID repetido");
        for (Object proceso : procesos) verificar(cola.desencolar() == proceso, "Orden FIFO incorrecto");
        verificar(cola.desencolar() == null && cola.getCantidad() == 0, "Cola vacía incorrecta");
        Proceso proceso = new Proceso("Prueba", 1, TipoProceso.CPU_BOUND, 0, 16, 1, 2);
        proceso.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
        proceso.cambiarEstado(EstadoProceso.EJECUCION, MotivoBloqueo.NINGUNO);
        proceso.ejecutarInstruccion();
        proceso.descontarDeadline();
        verificar(proceso.getPc() == 1 && proceso.getMar() == 1 && proceso.getRestante() == 0, "Contexto incorrecto");
        proceso.cambiarEstado(EstadoProceso.TERMINADO, MotivoBloqueo.NINGUNO);
        proceso.descontarDeadline();
        verificar(proceso.getDeadline() == 1, "Deadline de terminado alterado");
        boolean rechazo = false;
        try { proceso.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO); }
        catch (IllegalStateException esperado) { rechazo = true; }
        verificar(rechazo, "Un terminado no debe volver a listos");
        rechazo = false;
        try { new Proceso("", 1, TipoProceso.CPU_BOUND, 0, 1, 1, 1); }
        catch (IllegalArgumentException esperado) { rechazo = true; }
        verificar(rechazo, "Nombre vacío aceptado");
        System.out.println("OK: concurrencia, ID global, FIFO, PCB, estados y validación");
    }
}
