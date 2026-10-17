package Modelo;

import java.util.concurrent.Semaphore;

/**
 * Bloque de control de un proceso monohilo.
 * @author Adolfo
 */
public class Proceso {
    private static final Semaphore MUTEX_ID = new Semaphore(1, true);
    private static long siguienteId = 1;
    private final long id;
    private final String nombre;
    private final int computador;
    private final TipoProceso tipo;
    private final int prioridad;
    private final int memoria;
    private final int instrucciones;
    private final int deadlineInicial;
    private EstadoProceso estado;
    private MotivoBloqueo motivoBloqueo;
    private int pc;
    private int mar;
    private int restante;
    private int deadline;

    public Proceso(String nombre, int computador, TipoProceso tipo, int prioridad,
                   int memoria, int instrucciones, int deadline) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo es obligatorio");
        }
        if (computador < 1 || prioridad < 0 || memoria < 1 || instrucciones < 1 || deadline < 1) {
            throw new IllegalArgumentException("Los atributos del proceso estan fuera de rango");
        }
        MUTEX_ID.acquireUninterruptibly();
        try {
            if (siguienteId == Long.MAX_VALUE) {
                throw new IllegalStateException("Se agotaron los identificadores de proceso");
            }
            this.id = siguienteId++;
        } finally {
            MUTEX_ID.release();
        }
        this.nombre = nombre;
        this.computador = computador;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.memoria = memoria;
        this.instrucciones = instrucciones;
        this.deadlineInicial = deadline;
        this.deadline = deadline;
        this.restante = instrucciones;
        this.estado = EstadoProceso.NUEVO;
        this.motivoBloqueo = MotivoBloqueo.NINGUNO;
        this.pc = 0;
        this.mar = 0;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public int getComputador() { return computador; }
    public TipoProceso getTipo() { return tipo; }
    public int getPrioridad() { return prioridad; }
    public int getMemoria() { return memoria; }
    public int getInstrucciones() { return instrucciones; }
    public int getDeadlineInicial() { return deadlineInicial; }
    public EstadoProceso getEstado() { return estado; }
    public MotivoBloqueo getMotivoBloqueo() { return motivoBloqueo; }
    public int getPc() { return pc; }
    public int getMar() { return mar; }
    public int getRestante() { return restante; }
    public int getDeadline() { return deadline; }
}
