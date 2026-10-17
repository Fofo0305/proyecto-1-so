package Sistema;

import Modelo.Memoria;
import Modelo.Proceso;
import EDD.Cola;
import Modelo.EstadoProceso;
import Modelo.MotivoBloqueo;
import java.util.concurrent.Semaphore;

/**
 * Computador reutilizable con identificación y RAM propias.
 * @author Adolfo
 */
public class Computador {
    private final Semaphore mutex = new Semaphore(1, true);
    private final int id;
    private final Memoria memoria;
    private final Cola<Proceso> nuevos = new Cola<>();
    private final Cola<Proceso> listos = new Cola<>();
    private final Cola<Proceso> bloqueados = new Cola<>();
    private final Cola<Proceso> terminados = new Cola<>();

    public Computador(int id, int capacidadRam) {
        if (id < 1) {
            throw new IllegalArgumentException("El identificador del computador debe ser positivo");
        }
        this.id = id;
        this.memoria = new Memoria(capacidadRam);
    }

    /**
     * Recibe un proceso asignado a este computador y lo deja en nuevos sin reservar RAM.
     * @param proceso proceso en estado NUEVO que se incorpora
     */
    public void recibirProceso(Proceso proceso) {
        if (proceso == null) {
            throw new IllegalArgumentException("El proceso es obligatorio");
        }
        mutex.acquireUninterruptibly();
        try {
            if (proceso.getComputador() != id || proceso.getEstado() != EstadoProceso.NUEVO) {
                throw new IllegalArgumentException("El proceso debe ser nuevo y pertenecer a este computador");
            }
            if (contiene(nuevos, proceso.getId()) || contiene(listos, proceso.getId())
                    || contiene(bloqueados, proceso.getId()) || contiene(terminados, proceso.getId())) {
                throw new IllegalArgumentException("El proceso ya pertenece al computador");
            }
            nuevos.agregar(proceso);
        } finally {
            mutex.release();
        }
    }

    /**
     * Revisa una vez los nuevos y admite los que caben en RAM, en orden de llegada.
     * Los que no caben permanecen en nuevos conservando su orden relativo.
     * @return cantidad de procesos que pasaron a listos
     */
    public int admitirProcesos() {
        mutex.acquireUninterruptibly();
        try {
            int admitidos = 0;
            int pendientes = nuevos.getTamano();
            for (int i = 0; i < pendientes; i++) {
                Proceso proceso = nuevos.primero();
                if (memoria.reservar("proceso:" + proceso.getId(), proceso.getMemoria())) {
                    proceso.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
                    nuevos.quitar();
                    listos.agregar(proceso);
                    admitidos++;
                } else {
                    nuevos.agregar(nuevos.quitar());
                }
            }
            return admitidos;
        } finally {
            mutex.release();
        }
    }

    private boolean contiene(Cola<Proceso> cola, long idProceso) {
        for (Object elemento : cola.instantanea()) {
            if (((Proceso) elemento).getId() == idProceso) return true;
        }
        return false;
    }

    public int getId() { return id; }
    public Memoria getMemoria() { return memoria; }

    /** Consulta los procesos nuevos sin exponer la cola interna. */
    public Proceso[] getNuevos() { return copiarProcesos(nuevos); }

    /** Consulta los procesos listos sin exponer la cola interna. */
    public Proceso[] getListos() { return copiarProcesos(listos); }

    /** Consulta los procesos bloqueados sin exponer la cola interna. */
    public Proceso[] getBloqueados() { return copiarProcesos(bloqueados); }

    /** Consulta los procesos terminados sin exponer la cola interna. */
    public Proceso[] getTerminados() { return copiarProcesos(terminados); }

    private Proceso[] copiarProcesos(Cola<Proceso> cola) {
        mutex.acquireUninterruptibly();
        try {
            Object[] elementos = cola.instantanea();
            Proceso[] copia = new Proceso[elementos.length];
            for (int i = 0; i < elementos.length; i++) {
                copia[i] = (Proceso) elementos[i];
            }
            return copia;
        } finally {
            mutex.release();
        }
    }
}
