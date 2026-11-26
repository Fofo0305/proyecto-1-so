package Modelo;

import java.util.concurrent.Semaphore;

/** Bloque de control de un proceso monohilo. @author Adolfo */
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
    private EstadoProceso estado = EstadoProceso.NUEVO;
    private MotivoBloqueo motivoBloqueo = MotivoBloqueo.NINGUNO;
    private int pc;
    private int mar;
    private int restante;
    private int deadline;
    private ParametrosES parametrosES;
    private OperacionES operacionES;
    private String rutaObjetivo;
    private ParametrosArchivo parametrosArchivo;
    private long solicitudDisco;
    private boolean operacionAtendida;
    private int ejecutadasRafaga;
    private long esHasta;
    private long llegada;
    private long primeraRespuesta = -1;
    private String resultado = "ACTIVO";

    public Proceso(String nombre, int computador, TipoProceso tipo, int prioridad,
            int memoria, int instrucciones, int deadline) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (tipo == null) throw new IllegalArgumentException("El tipo es obligatorio");
        if (computador != 1 || prioridad < 0 || memoria < 1 || instrucciones < 1 || deadline < 1)
            throw new IllegalArgumentException("Los atributos del proceso estan fuera de rango");
        MUTEX_ID.acquireUninterruptibly();
        try {
            if (siguienteId == Long.MAX_VALUE) throw new IllegalStateException("Se agotaron los identificadores de proceso");
            id = siguienteId++;
        } finally { MUTEX_ID.release(); }
        this.nombre = nombre;
        this.computador = computador;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.memoria = memoria;
        this.instrucciones = instrucciones;
        this.deadlineInicial = deadline;
        this.deadline = deadline;
        this.restante = instrucciones;
    }

    public void configurarES(ParametrosES parametros) {
        if (estado != EstadoProceso.NUEVO || tipo != TipoProceso.IO_BOUND || parametros == null
                || parametrosES != null || operacionES != null)
            throw new IllegalStateException("E/S solo al crear IO_BOUND");
        parametrosES = parametros;
    }

    /** Asocia una solicitud de disco al PCB antes de admitir el proceso. */
    public void configurarOperacionDisco(OperacionES operacion, String ruta) {
        configurarOperacionArchivo(operacion, ruta, ParametrosArchivo.ADMINISTRADOR);
    }

    /**
     * Asocia una operacion de disco con los datos y la identidad de quien creo el proceso,
     * para que el sistema de archivos aplique sus permisos cuando la atienda.
     */
    public void configurarOperacionArchivo(OperacionES operacion, String ruta, ParametrosArchivo datos) {
        if (estado != EstadoProceso.NUEVO || tipo != TipoProceso.IO_BOUND || operacion == null
                || datos == null || parametrosES != null || operacionES != null)
            throw new IllegalStateException("La operacion de disco solo se configura una vez en un IO_BOUND nuevo");
        validarDestino(operacion, ruta);
        operacionES = operacion;
        rutaObjetivo = ruta;
        parametrosArchivo = datos;
    }

    /** Un proceso CRUD pide su operacion una sola vez, en su primera instruccion. */
    public boolean necesitaSolicitarArchivo() {
        return operacionES != null && operacionES.requiereRuta() && solicitudDisco == 0 && !operacionAtendida;
    }

    public void registrarSolicitudDisco(long idSolicitud) {
        if (idSolicitud < 1 || !necesitaSolicitarArchivo())
            throw new IllegalStateException("El proceso no tiene una solicitud de archivo pendiente");
        solicitudDisco = idSolicitud;
    }

    /** Marca la operacion como resuelta: atendida por el disco o rechazada por permisos. */
    public void cerrarOperacionArchivo() {
        if (operacionES == null || !operacionES.requiereRuta() || operacionAtendida)
            throw new IllegalStateException("El proceso no tiene una operacion de archivo abierta");
        operacionAtendida = true;
    }

    /** Indica si el PCB ya puede entrar a las colas del computador. */
    public boolean tieneConfiguracionValida() {
        boolean esTemporal = parametrosES != null;
        boolean esDisco = operacionES != null;
        return tipo == TipoProceso.CPU_BOUND ? !esTemporal && !esDisco : esTemporal != esDisco;
    }

    static void validarDestino(OperacionES operacion, String ruta) {
        if (operacion.requiereRuta()) {
            if (ruta == null || ruta.isBlank() || ruta.charAt(0) != '/')
                throw new IllegalArgumentException("La ruta objetivo debe ser absoluta");
        } else if (ruta != null) {
            throw new IllegalArgumentException("Suspender y reactivar no utilizan una ruta de archivo");
        }
    }

    public boolean agotarRafaga(long ciclo) {
        if (tipo != TipoProceso.IO_BOUND || parametrosES == null || restante == 0) return false;
        if (++ejecutadasRafaga < parametrosES.rafaga()) return false;
        ejecutadasRafaga = 0;
        esHasta = ciclo + parametrosES.duracion();
        return true;
    }

    public void cambiarEstado(EstadoProceso nuevo, MotivoBloqueo motivo) {
        if (nuevo == null || motivo == null) throw new IllegalArgumentException("El estado y el motivo son obligatorios");
        boolean destinoBloqueado = nuevo == EstadoProceso.BLOQUEADO
                || nuevo == EstadoProceso.BLOQUEADO_SUSPENDIDO;
        if (destinoBloqueado != (motivo != MotivoBloqueo.NINGUNO))
            throw new IllegalArgumentException("El motivo no corresponde al estado");
        if (!estado.permiteTransicionA(nuevo))
            throw new IllegalStateException("Transicion no permitida: " + estado + " -> " + nuevo);
        estado = nuevo;
        motivoBloqueo = motivo;
    }

    public void ejecutarInstruccion() {
        if (estado != EstadoProceso.EJECUCION || restante == 0 || deadline == 0)
            throw new IllegalStateException("El proceso no puede ejecutar una instruccion");
        pc++;
        mar++;
        restante--;
    }

    public void descontarDeadline() { if (estado != EstadoProceso.TERMINADO && deadline > 0) deadline--; }
    public void registrarLlegada(long ciclo) { llegada = ciclo; }
    public void registrarRespuesta(long ciclo) { if (primeraRespuesta < 0) primeraRespuesta = ciclo - llegada - 1; }
    public void cerrarResultado() { resultado = restante == 0 ? "COMPLETADO" : deadline == 0 ? "VENCIDO" : "CANCELADO"; }

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
    public ParametrosES getParametrosES() { return parametrosES; }
    public OperacionES getOperacionES() { return operacionES; }
    public String getRutaObjetivo() { return rutaObjetivo; }
    public ParametrosArchivo getParametrosArchivo() { return parametrosArchivo; }
    public long getSolicitudDisco() { return solicitudDisco; }
    public boolean isOperacionAtendida() { return operacionAtendida; }
    public long getEsHasta() { return esHasta; }
    public long getRespuesta() { return primeraRespuesta; }
    public String getResultado() { return resultado; }
}
