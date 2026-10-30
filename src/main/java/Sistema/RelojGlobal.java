package Sistema;

import java.util.concurrent.Semaphore;

/**
 * Coordina los hilos: todos confirman un ciclo antes de iniciar el siguiente.
 * @author Adolfo
 */
public class RelojGlobal implements AutoCloseable {
    private final Semaphore mutex = new Semaphore(1, true);
    private final TrabajadorReloj[] trabajadores;
    private final FinalizadorCiclo finalizador;
    private long ciclo;
    private boolean cerrado;
    private final Semaphore despertar = new Semaphore(0);
    private volatile Thread motor;
    private volatile boolean pausado;
    private volatile boolean detenido;
    private volatile int duracionMs = 100;
    private volatile Throwable errorAutomatico;

    public RelojGlobal(ComponenteReloj[] componentes) {
        this(componentes, ciclo -> { });
    }

    public RelojGlobal(ComponenteReloj[] componentes, FinalizadorCiclo finalizador) {
        if (finalizador == null) throw new IllegalArgumentException("El finalizador es obligatorio");
        this.finalizador = finalizador;
        if (componentes == null || componentes.length < 2)
            throw new IllegalArgumentException("Se requieren al menos dos componentes");
        for (int i = 0; i < componentes.length; i++) {
            if (componentes[i] == null) throw new IllegalArgumentException("Componente nulo");
            for (int j = 0; j < i; j++)
                if (componentes[i] == componentes[j]) throw new IllegalArgumentException("Componente repetido");
            if (componentes[i] instanceof Computador computador && computador.getUltimoCiclo() != 0)
                throw new IllegalArgumentException("El computador debe iniciar en ciclo cero");
        }
        trabajadores = new TrabajadorReloj[componentes.length];
        for (int i = 0; i < componentes.length; i++) trabajadores[i] = new TrabajadorReloj(componentes[i], i + 1);
    }

    /** Avanza un ciclo completo y propaga los fallos de los computadores. */
    public void avanzarCiclo() {
        mutex.acquireUninterruptibly();
        try {
            if (cerrado) throw new IllegalStateException("El reloj esta cerrado");
            if (motor != null && Thread.currentThread() != motor)
                throw new IllegalStateException("El reloj automatico controla los ciclos");
            if (Thread.currentThread() == motor && (pausado || detenido)) return;
            long siguiente = ciclo + 1;
            for (TrabajadorReloj trabajador : trabajadores) trabajador.solicitar(siguiente);
            for (TrabajadorReloj trabajador : trabajadores) trabajador.esperar();
            for (TrabajadorReloj trabajador : trabajadores) {
                if (trabajador.getError() != null) {
                    cerrarTrabajadores();
                    throw new IllegalStateException("Fallo un componente del reloj", trabajador.getError());
                }
            }
            try { finalizador.finalizarCiclo(siguiente); }
            catch (RuntimeException | Error fallo) {
                cerrarTrabajadores();
                throw fallo;
            }
            ciclo = siguiente;
        } finally { mutex.release(); }
    }

    public long getCiclo() {
        mutex.acquireUninterruptibly();
        try { return ciclo; } finally { mutex.release(); }
    }

    /** Inicia el reloj automático una sola vez; cerrar es definitivo. */
    public synchronized void iniciar() {
        mutex.acquireUninterruptibly();
        try {
            if (cerrado || detenido || motor != null) throw new IllegalStateException("El reloj no puede iniciarse");
            motor = new Thread(this::ejecutarAutomatico, "Reloj-global");
            motor.setDaemon(true);
            motor.start();
        } finally { mutex.release(); }
    }

    private void ejecutarAutomatico() {
        try {
            while (!detenido) {
                if (pausado) {
                    despertar.acquireUninterruptibly();
                    continue;
                }
                avanzarCiclo();
                try { Thread.sleep(duracionMs); }
                catch (InterruptedException interrupcion) {
                    if (!detenido) throw new IllegalStateException("Motor del reloj interrumpido", interrupcion);
                }
            }
        } catch (Throwable fallo) {
            errorAutomatico = fallo;
        } finally {
            close();
        }
    }

    /** Pausa en el límite de un ciclo completo, sin dejar computadores a medias. */
    public void pausar() {
        mutex.acquireUninterruptibly();
        try {
            if (cerrado || detenido || motor == null) throw new IllegalStateException("El reloj no esta activo");
            pausado = true;
        } finally { mutex.release(); }
    }

    public void reanudar() {
        mutex.acquireUninterruptibly();
        try {
            if (cerrado || detenido || motor == null) throw new IllegalStateException("El reloj no esta activo");
            pausado = false;
            despertar.release();
        } finally { mutex.release(); }
    }

    public void setDuracionMs(int duracionMs) {
        if (duracionMs < 1) throw new IllegalArgumentException("La duracion debe ser positiva");
        this.duracionMs = duracionMs;
    }

    public int getDuracionMs() { return duracionMs; }
    public Throwable getError() { return errorAutomatico; }
    public boolean estaActivo() { Thread hilo = motor; return hilo != null && hilo.isAlive(); }

    /** Detiene el motor y libera sus trabajadores; equivale a cerrar el reloj. */
    public void detener() { close(); }

    private void cerrarTrabajadores() {
        cerrado = true;
        for (TrabajadorReloj trabajador : trabajadores) trabajador.detener();
        for (TrabajadorReloj trabajador : trabajadores) trabajador.esperarCierre();
    }

    @Override
    public void close() {
        detenido = true;
        despertar.release();
        Thread hilo = motor;
        if (hilo != null && hilo != Thread.currentThread()) {
            // La interrupción solo adelanta el final de la espera entre ciclos.
            hilo.interrupt();
            boolean interrumpido = false;
            while (hilo.isAlive()) {
                try { hilo.join(); }
                catch (InterruptedException interrupcion) { interrumpido = true; }
            }
            if (interrumpido) Thread.currentThread().interrupt();
        }
        mutex.acquireUninterruptibly();
        try { if (!cerrado) cerrarTrabajadores(); } finally { mutex.release(); }
    }
}
