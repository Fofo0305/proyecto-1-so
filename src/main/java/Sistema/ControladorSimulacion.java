package Sistema;

import Modelo.Configuracion;
import Modelo.Proceso;
import Modelo.TipoProceso;
import java.util.concurrent.Semaphore;

/**
 * Construye y conserva los componentes de la simulación fuera de las ventanas.
 * Las operaciones que esperan hilos deben ejecutarse fuera del EDT de Swing.
 * @author Adolfo
 */
public class ControladorSimulacion implements AutoCloseable {
    private final Semaphore mutex = new Semaphore(1, true);
    private Configuracion configuracion;
    private Computador[] computadores;
    private RelojGlobal reloj;
    private boolean cerrado;

    /** Reemplaza la simulación únicamente si el reloj anterior está detenido o no iniciado. */
    public void configurar(Configuracion nueva) {
        if (nueva == null) throw new IllegalArgumentException("La configuracion es obligatoria");
        mutex.acquireUninterruptibly();
        try {
            if (cerrado) throw new IllegalStateException("El controlador esta cerrado");
            if (reloj != null && reloj.estaActivo())
                throw new IllegalStateException("Detenga la simulacion antes de cambiar la configuracion");
            int[] capacidades = nueva.getRam();
            Computador[] nuevos = new Computador[capacidades.length];
            for (int i = 0; i < nuevos.length; i++) nuevos[i] = new Computador(i + 1, capacidades[i]);
            RelojGlobal nuevoReloj = new RelojGlobal(nuevos);
            nuevoReloj.setDuracionMs(nueva.getDuracionMs());
            if (reloj != null) reloj.close();
            configuracion = nueva;
            computadores = nuevos;
            reloj = nuevoReloj;
        } finally { mutex.release(); }
    }

    /** Crea un proceso CPU-bound en el computador elegido y solicita su admisión. */
    public Proceso crearProceso(String nombre, int computador, int prioridad,
                                int memoria, int instrucciones, int deadline) {
        mutex.acquireUninterruptibly();
        try {
            if (cerrado || computadores == null) throw new IllegalStateException("Configure primero el cluster");
            if (computador < 1 || computador > computadores.length)
                throw new IllegalArgumentException("Seleccione un computador existente");
            Computador destino = computadores[computador - 1];
            if (memoria > destino.getMemoria().getCapacidad())
                throw new IllegalArgumentException("La memoria del proceso supera la RAM total del computador");
            Proceso proceso = new Proceso(nombre, computador, TipoProceso.CPU_BOUND, prioridad, memoria, instrucciones, deadline);
            destino.recibirProceso(proceso);
            destino.admitirProcesos();
            return proceso;
        } finally { mutex.release(); }
    }

    public Configuracion getConfiguracion() {
        mutex.acquireUninterruptibly();
        try { return configuracion; } finally { mutex.release(); }
    }

    /** Devuelve un arreglo independiente; las instancias pertenecen al controlador. */
    public Computador[] getComputadores() {
        mutex.acquireUninterruptibly();
        try { return computadores == null ? new Computador[0] : computadores.clone(); }
        finally { mutex.release(); }
    }

    public long getCiclo() {
        mutex.acquireUninterruptibly();
        try { return reloj == null ? 0 : reloj.getCiclo(); } finally { mutex.release(); }
    }

    @Override
    public void close() {
        mutex.acquireUninterruptibly();
        try {
            cerrado = true;
            if (reloj != null) reloj.close();
        } finally { mutex.release(); }
    }
}
