package Modelo;

import java.util.concurrent.Semaphore;

/**
 * Bloque de control de un proceso monohilo.
 * El núcleo debe serializar las transiciones y las lecturas del contexto.
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
    private final ParametrosBuffer parametrosBuffer;
    private final Buffer<Object> buffer;
    private final ContextoBuffer contextoBuffer;
    private EstadoProceso estado;
    private MotivoBloqueo motivoBloqueo;
    private int pc;
    private int mar;
    private int restante;
    private int deadline;
    private ParametrosES parametrosES = new ParametrosES(2,3);
    private int ejecutadasRafaga;
    private long esHasta;
    private long llegada;
    private long primeraRespuesta = -1;
    private String resultado = "ACTIVO";
    public void configurarES(ParametrosES p) { if (estado != EstadoProceso.NUEVO || tipo != TipoProceso.IO_BOUND || p==null) throw new IllegalStateException("E/S solo al crear IO_BOUND"); parametrosES=p; }
    public ParametrosES getParametrosES() { return parametrosES; }
    public void registrarLlegada(long ciclo) { llegada=ciclo; }
    public void registrarRespuesta(long ciclo) { if(primeraRespuesta<0) primeraRespuesta=ciclo-llegada-1; }
    public long getRespuesta() { return primeraRespuesta; }
    public void cerrarResultado() { resultado=restante==0?"COMPLETADO":deadline==0?"VENCIDO":"CANCELADO"; }
    public String getResultado() { return resultado; }
    public boolean agotarRafaga(long ciclo) {
        if(tipo!=TipoProceso.IO_BOUND || restante==0) return false;
        if(++ejecutadasRafaga < parametrosES.rafaga()) return false;
        ejecutadasRafaga=0; esHasta=ciclo+parametrosES.duracion(); return true;
    }
    public long getEsHasta() { return esHasta; }

    public Proceso(String nombre, int computador, TipoProceso tipo, int prioridad,
                   int memoria, int instrucciones, int deadline) {
        this(nombre, computador, tipo, prioridad, memoria, instrucciones, deadline, null, null);
    }

    public Proceso(String nombre, int computador, TipoProceso tipo, int prioridad,
                   int memoria, int instrucciones, int deadline, ParametrosBuffer parametrosBuffer,
                   Buffer<Object> buffer) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo es obligatorio");
        }
        if (computador < 1 || prioridad < 0 || memoria < 1 || instrucciones < 1 || deadline < 1) {
            throw new IllegalArgumentException("Los atributos del proceso estan fuera de rango");
        }
        boolean comunicante = tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR;
        if (comunicante) {
            if (parametrosBuffer == null || buffer == null || !parametrosBuffer.buffer().equals(buffer.getNombre())
                    || instrucciones != parametrosBuffer.instrucciones())
                throw new IllegalArgumentException("Se requiere un buffer y parametros consistentes");
        } else if (parametrosBuffer != null || buffer != null) {
            throw new IllegalArgumentException("El proceso no utiliza buffer");
        }
        this.parametrosBuffer = parametrosBuffer;
        this.buffer = buffer;
        this.contextoBuffer = comunicante ? new ContextoBuffer(buffer, parametrosBuffer, tipo) : null;
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

    /**
     * Cambia el estado siguiendo el ciclo de vida del proceso.
     * Solo un proceso bloqueado puede tener un motivo distinto de NINGUNO.
     * @param nuevo estado solicitado
     * @param motivo causa del bloqueo, o NINGUNO
     */
    public void cambiarEstado(EstadoProceso nuevo, MotivoBloqueo motivo) {
        if (nuevo == null || motivo == null) {
            throw new IllegalArgumentException("El estado y el motivo son obligatorios");
        }
        if ((nuevo == EstadoProceso.BLOQUEADO) != (motivo != MotivoBloqueo.NINGUNO)) {
            throw new IllegalArgumentException("El motivo no corresponde al estado");
        }
        boolean permitido = switch (estado) {
            case NUEVO -> nuevo == EstadoProceso.LISTO || nuevo == EstadoProceso.TERMINADO;
            case LISTO -> nuevo == EstadoProceso.EJECUCION || nuevo == EstadoProceso.TERMINADO;
            case EJECUCION -> nuevo == EstadoProceso.LISTO || nuevo == EstadoProceso.BLOQUEADO
                    || nuevo == EstadoProceso.TERMINADO;
            case BLOQUEADO -> nuevo == EstadoProceso.LISTO || nuevo == EstadoProceso.TERMINADO;
            case TERMINADO -> false;
        };
        if (!permitido) {
            throw new IllegalStateException("Transicion no permitida: " + estado + " -> " + nuevo);
        }
        estado = nuevo;
        motivoBloqueo = motivo;
    }

    /** Ejecuta una instrucción cuando el proceso tiene CPU, trabajo y plazo disponibles. */
    public void ejecutarInstruccion() {
        if (estado != EstadoProceso.EJECUCION || restante == 0 || deadline == 0) {
            throw new IllegalStateException("El proceso no puede ejecutar una instruccion");
        }
        pc++;
        mar++;
        restante--;
    }

    /**
     * Descuenta un ciclo del plazo, también en NUEVO o BLOQUEADO, sin bajar de cero.
     * El núcleo debe llamarlo una vez por ciclo, después de la instrucción.
     * Si finalización y vencimiento coinciden, el núcleo prioriza la finalización.
     * Este método no termina procesos ni modifica sus colas.
     */
    public void descontarDeadline() {
        if (estado != EstadoProceso.TERMINADO && deadline > 0) {
            deadline--;
        }
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
    public ParametrosBuffer getParametrosBuffer() { return parametrosBuffer; }
    public Buffer<Object> getBuffer() { return buffer; }
    public ContextoBuffer getContextoBuffer() { return contextoBuffer; }
}
