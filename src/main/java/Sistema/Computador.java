package Sistema;

import Modelo.Memoria;
import Modelo.Proceso;
import EDD.Cola;
import Modelo.EstadoProceso;
import Modelo.MotivoBloqueo;
import Modelo.ModoCPU;
import Modelo.TipoProceso;
import Planificacion.Politica;
import Planificacion.FCFS;
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
    private final CPU cpu = new CPU();
    private final Politica politica = new FCFS();
    private long ultimoCiclo;

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
                    || contiene(bloqueados, proceso.getId()) || contiene(terminados, proceso.getId())
                    || cpu.getActual() == proceso) {
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
            return admitirPendientes();
        } finally {
            mutex.release();
        }
    }

    private int admitirPendientes() {
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
    }

    /**
     * Termina un proceso registrado, libera su reserva y vuelve a revisar los nuevos.
     * El núcleo solicita esta operación al finalizar, vencer o cancelar un proceso.
     * @param proceso proceso que se retira de las colas activas
     * @return false si ya estaba en terminados; true si se terminó ahora
     */
    public boolean terminarProceso(Proceso proceso) {
        if (proceso == null || proceso.getComputador() != id) {
            throw new IllegalArgumentException("El proceso debe pertenecer a este computador");
        }
        mutex.acquireUninterruptibly();
        try {
            boolean resultado = terminarRegistrado(proceso);
            admitirPendientes();
            return resultado;
        } finally {
            mutex.release();
        }
    }

    private boolean terminarRegistrado(Proceso proceso) {
        if (contiene(terminados, proceso.getId())) return false;
        if (!contiene(nuevos, proceso.getId()) && !contiene(listos, proceso.getId())
                && !contiene(bloqueados, proceso.getId()) && cpu.getActual() != proceso)
            throw new IllegalArgumentException("El proceso no esta registrado en el computador");
        proceso.cambiarEstado(EstadoProceso.TERMINADO, MotivoBloqueo.NINGUNO);
        retirar(nuevos, proceso.getId());
        retirar(listos, proceso.getId());
        retirar(bloqueados, proceso.getId());
        if (cpu.getActual() == proceso) cpu.retirar();
        memoria.liberar("proceso:" + proceso.getId());
        terminados.agregar(proceso);
        return true;
    }

    /** Ejecuta una instrucción FCFS y descuenta una vez todos los plazos del ciclo. */
    public void avanzarCiclo(long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            if (ciclo != ultimoCiclo + 1) throw new IllegalArgumentException("El ciclo debe ser consecutivo");
            for (Object elemento : nuevos.instantanea()) validarTipoEjecucion((Proceso) elemento);
            for (Object elemento : listos.instantanea()) validarTipoEjecucion((Proceso) elemento);
            admitirPendientes();
            Object[][] colasCiclo = {nuevos.instantanea(), listos.instantanea(), bloqueados.instantanea()};
            Proceso anterior = cpu.getActual();
            if (anterior == null) {
                Proceso seleccionado = politica.seleccionar(listos);
                if (seleccionado != null) cpu.asignar(seleccionado);
            }
            cpu.avanzarCiclo(ciclo);
            for (Object[] cola : colasCiclo)
                for (Object elemento : cola) ((Proceso) elemento).descontarDeadline();
            if (anterior != null) anterior.descontarDeadline();
            Proceso ejecutado = cpu.getActual();
            if (ejecutado != null && ejecutado.getRestante() == 0) terminarRegistrado(ejecutado);
            for (Object[] cola : colasCiclo) {
                for (Object elemento : cola) {
                    Proceso proceso = (Proceso) elemento;
                    if (proceso.getEstado() != EstadoProceso.TERMINADO && proceso.getDeadline() == 0)
                        terminarRegistrado(proceso);
                }
            }
            if (anterior != null && anterior.getEstado() != EstadoProceso.TERMINADO && anterior.getDeadline() == 0)
                terminarRegistrado(anterior);
            admitirPendientes();
            ultimoCiclo = ciclo;
        } finally { mutex.release(); }
    }

    private void validarTipoEjecucion(Proceso proceso) {
        if (proceso.getTipo() != TipoProceso.CPU_BOUND)
            throw new UnsupportedOperationException("Este flujo solo ejecuta CPU_BOUND; falta el modulo de " + proceso.getTipo());
    }

    public Proceso getProcesoEnCPU() {
        mutex.acquireUninterruptibly();
        try { return cpu.getActual(); } finally { mutex.release(); }
    }

    public ModoCPU getModoCPU() {
        mutex.acquireUninterruptibly();
        try { return cpu.getModo(); } finally { mutex.release(); }
    }

    public long getUltimoCiclo() {
        mutex.acquireUninterruptibly();
        try { return ultimoCiclo; } finally { mutex.release(); }
    }

    private void retirar(Cola<Proceso> cola, long idProceso) {
        int cantidad = cola.getTamano();
        for (int i = 0; i < cantidad; i++) {
            Proceso proceso = cola.quitar();
            if (proceso.getId() != idProceso) cola.agregar(proceso);
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
