package Pruebas;

import Modelo.Buffer;
import Modelo.Proceso;
import Modelo.SemaforoSimulado;
import Modelo.TipoProceso;

/** Permisos, FIFO y concurrencia real sin colecciones externas. @author Luciano Rojas */
public final class PruebaSemaforos {
    static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    static Proceso proceso(String nombre) {
        return new Proceso(nombre, 1, TipoProceso.CPU_BOUND, 0, 1, 10, 50);
    }

    static void rechazar(Runnable accion) {
        try {
            accion.run();
            throw new AssertionError("Se acepto una operacion invalida");
        } catch (IllegalArgumentException | IllegalStateException esperado) { }
    }

    public static void main(String[] args) throws Exception {
        rechazar(() -> new SemaforoSimulado("", 0, 1));
        rechazar(() -> new SemaforoSimulado("s", -1, 1));
        rechazar(() -> new SemaforoSimulado("s", 2, 1));
        rechazar(() -> new SemaforoSimulado("s", 0, 0));
        Buffer<Integer> buffer = new Buffer<>("B", 3, 1);
        verificar(buffer.getMutexSimulado().getValor() == 1
                && buffer.getVacios().getValor() == 3 && buffer.getLlenos().getValor() == 0,
                "Valores iniciales incorrectos");
        SemaforoSimulado semaforo = buffer.getMutexSimulado();
        Proceso titular = proceso("Titular"), primero = proceso("Primero"),
                cancelado = proceso("Cancelado"), ultimo = proceso("Ultimo"), nuevo = proceso("Nuevo");
        verificar(semaforo.esperar(titular), "No adquirio el permiso inicial");
        verificar(!semaforo.esperar(primero) && !semaforo.esperar(cancelado)
                && !semaforo.esperar(ultimo), "No encolo las esperas");
        rechazar(() -> semaforo.esperar(primero));
        rechazar(() -> semaforo.esperar(null));
        Proceso[] copia = semaforo.getEsperando();
        copia[0] = null;
        verificar(semaforo.getEsperando()[0] == primero, "Se expuso la cola interna");
        verificar(semaforo.cancelarEspera(cancelado) && !semaforo.cancelarEspera(cancelado),
                "Cancelacion incorrecta");
        verificar(semaforo.getValor() == 0, "Cancelar una espera creo un permiso");
        verificar(semaforo.senalizar() == primero && semaforo.getValor() == 0,
                "No transfirio directamente al primero");
        verificar(!semaforo.esperar(nuevo), "Un nuevo proceso robo el permiso transferido");
        verificar(semaforo.senalizar() == ultimo && semaforo.senalizar() == nuevo,
                "La cancelacion o llegada nueva altero FIFO");
        verificar(semaforo.senalizar() == null && semaforo.getValor() == 1,
                "No devolvio el permiso sin esperas");
        rechazar(semaforo::senalizar);
        verificar(semaforo.getValor() == 1 && semaforo.getEsperando().length == 0,
                "La senal excesiva altero el semaforo");
        comprobarConcurrencia();
        System.out.println("OK: inicializacion, permisos, FIFO, cancelacion y concurrencia de semaforos");
    }

    private static void comprobarConcurrencia() throws Exception {
        SemaforoSimulado semaforo = new SemaforoSimulado("Concurrente", 0, 2000);
        ejecutarConcurrentes(() -> { for (int i = 0; i < 500; i++) semaforo.senalizar(); });
        verificar(semaforo.getValor() == 2000, "Se perdieron senales concurrentes");
        ejecutarConcurrentes(() -> {
            Proceso proceso = proceso("Concurrente");
            for (int i = 0; i < 500; i++) verificar(semaforo.esperar(proceso), "Se perdio un permiso");
        });
        verificar(semaforo.getValor() == 0 && semaforo.getEsperando().length == 0,
                "Conteo incorrecto tras adquisiciones concurrentes");
    }

    private static void ejecutarConcurrentes(Runnable accion) throws Exception {
        java.util.concurrent.atomic.AtomicReference<Throwable> fallo =
                new java.util.concurrent.atomic.AtomicReference<>();
        Thread[] hilos = new Thread[4];
        for (int i = 0; i < hilos.length; i++) {
            hilos[i] = new Thread(accion);
            hilos[i].setDaemon(true);
            hilos[i].setUncaughtExceptionHandler((hilo, error) -> fallo.set(error));
            hilos[i].start();
        }
        for (Thread hilo : hilos) {
            hilo.join(5000);
            verificar(!hilo.isAlive(), "Operacion real detenida");
        }
        if (fallo.get() != null) throw new AssertionError("Fallo concurrente", fallo.get());
    }
}
