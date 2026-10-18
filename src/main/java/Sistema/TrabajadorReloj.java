package Sistema;

import java.util.concurrent.Semaphore;

/**
 * Hilo de un componente: espera el ciclo y confirma su finalización.
 * @author Adolfo
 */
final class TrabajadorReloj implements Runnable {
    private final ComponenteReloj componente;
    private final Semaphore inicio = new Semaphore(0);
    private final Semaphore fin = new Semaphore(0);
    private final Thread hilo;
    private volatile boolean detenido;
    private volatile long ciclo;
    private volatile Throwable error;

    TrabajadorReloj(ComponenteReloj componente, int numero) {
        this.componente = componente;
        hilo = new Thread(this, "Computador-reloj-" + numero);
        hilo.setDaemon(true);
        hilo.start();
    }

    void solicitar(long ciclo) { this.ciclo = ciclo; inicio.release(); }
    void esperar() { fin.acquireUninterruptibly(); }
    Throwable getError() { return error; }
    void detener() { detenido = true; inicio.release(); }

    void esperarCierre() {
        boolean interrumpido = false;
        while (hilo.isAlive()) {
            try { hilo.join(); }
            catch (InterruptedException interrupcion) { interrumpido = true; }
        }
        if (interrumpido) Thread.currentThread().interrupt();
    }

    @Override
    public void run() {
        while (true) {
            inicio.acquireUninterruptibly();
            if (detenido) return;
            try { componente.avanzarCiclo(ciclo); }
            catch (Throwable fallo) { error = fallo; }
            finally { fin.release(); }
        }
    }
}
