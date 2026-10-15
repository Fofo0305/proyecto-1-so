package com.mycompany.proyecto1so2526.modelo;

import java.util.concurrent.Semaphore;

/** PCB monohilo. El núcleo debe proteger las transiciones y sus lecturas.
 * @author Adolfo
 */
public final class Proceso {
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
    private EstadoProceso estado = EstadoProceso.NUEVO;
    private MotivoBloqueo motivoBloqueo = MotivoBloqueo.NINGUNO;
    private int deadline;
    private int pc;
    private int mar;
    private int restante;

    public Proceso(String nombre, int computador, TipoProceso tipo, int prioridad,
                   int memoria, int instrucciones, int deadline) {
        if (nombre == null || nombre.isBlank() || tipo == null)
            throw new IllegalArgumentException("Nombre y tipo obligatorios");
        if (computador < 1 || prioridad < 0 || memoria < 1 || instrucciones < 1 || deadline < 1)
            throw new IllegalArgumentException("Atributos del proceso fuera de rango");
        MUTEX_ID.acquireUninterruptibly();
        try { id = siguienteId++; } finally { MUTEX_ID.release(); }
        this.nombre = nombre.trim();
        this.computador = computador;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.memoria = memoria;
        this.instrucciones = instrucciones;
        this.restante = instrucciones;
        this.deadline = deadline;
        this.deadlineInicial = deadline;
    }

    public void ejecutarInstruccion() {
        if (estado != EstadoProceso.EJECUCION || restante == 0 || deadline == 0)
            throw new IllegalStateException("El proceso no puede ejecutar");
        pc++;
        mar++;
        restante--;
    }

    /** Se descuenta una vez por ciclo, incluso en nuevos y bloqueados. */
    public void descontarDeadline() {
        if (estado != EstadoProceso.TERMINADO && deadline > 0) deadline--;
    }

    public void cambiarEstado(EstadoProceso nuevo, MotivoBloqueo motivo) {
        if (nuevo == null || motivo == null) throw new IllegalArgumentException("Estado y motivo obligatorios");
        if ((nuevo == EstadoProceso.BLOQUEADO) != (motivo != MotivoBloqueo.NINGUNO))
            throw new IllegalArgumentException("El motivo debe corresponder al bloqueo");
        boolean valido = switch (estado) {
            case NUEVO -> nuevo == EstadoProceso.LISTO || nuevo == EstadoProceso.TERMINADO;
            case LISTO -> nuevo == EstadoProceso.EJECUCION || nuevo == EstadoProceso.TERMINADO;
            case EJECUCION -> nuevo == EstadoProceso.LISTO || nuevo == EstadoProceso.BLOQUEADO || nuevo == EstadoProceso.TERMINADO;
            case BLOQUEADO -> nuevo == EstadoProceso.LISTO || nuevo == EstadoProceso.TERMINADO;
            case TERMINADO -> false;
        };
        if (!valido) throw new IllegalStateException("Transición inválida: " + estado + " -> " + nuevo);
        estado = nuevo;
        motivoBloqueo = motivo;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public int getComputador() { return computador; }
    public TipoProceso getTipo() { return tipo; }
    public int getPrioridad() { return prioridad; }
    public int getMemoria() { return memoria; }
    public int getInstrucciones() { return instrucciones; }
    public int getDeadlineInicial() { return deadlineInicial; }
    public int getDeadline() { return deadline; }
    public int getPc() { return pc; }
    public int getMar() { return mar; }
    public int getRestante() { return restante; }
    public EstadoProceso getEstado() { return estado; }
    public MotivoBloqueo getMotivoBloqueo() { return motivoBloqueo; }
}
