package Sistema;

import java.util.concurrent.Semaphore;

/**
 * Coordina los hilos: todos confirman un ciclo antes de iniciar el siguiente.
 * @author Adolfo
 */
public class RelojGlobal implements AutoCloseable {
    private final Semaphore mutex = new Semaphore(1, true);
    private final TrabajadorReloj[] trabajadores;
    private long ciclo;
    private boolean cerrado;

    public RelojGlobal(ComponenteReloj[] componentes) {
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
            long siguiente = ciclo + 1;
            for (TrabajadorReloj trabajador : trabajadores) trabajador.solicitar(siguiente);
            for (TrabajadorReloj trabajador : trabajadores) trabajador.esperar();
            for (TrabajadorReloj trabajador : trabajadores) {
                if (trabajador.getError() != null) {
                    cerrarTrabajadores();
                    throw new IllegalStateException("Fallo un componente del reloj", trabajador.getError());
                }
            }
            ciclo = siguiente;
        } finally { mutex.release(); }
    }

    public long getCiclo() {
        mutex.acquireUninterruptibly();
        try { return ciclo; } finally { mutex.release(); }
    }

    private void cerrarTrabajadores() {
        cerrado = true;
        for (TrabajadorReloj trabajador : trabajadores) trabajador.detener();
        for (TrabajadorReloj trabajador : trabajadores) trabajador.esperarCierre();
    }

    @Override
    public void close() {
        mutex.acquireUninterruptibly();
        try { if (!cerrado) cerrarTrabajadores(); } finally { mutex.release(); }
    }
}
