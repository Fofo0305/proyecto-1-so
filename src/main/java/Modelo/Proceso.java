package Modelo;

/**
 * Bloque de control de un proceso monohilo.
 * @author Adolfo
 */
public class Proceso {
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
