package Modelo;

/** Datos de un PCB capturados por el computador para dibujar su tarjeta. @author Adolfo */
public record VistaProceso(long id, String nombre, int memoria, int prioridad,
        int deadline, int restante, int pc, int mar, EstadoProceso estado,
        TipoProceso tipo, MotivoBloqueo motivoBloqueo, String operacion) {
    public VistaProceso(long id, String nombre, int memoria, int prioridad,
            int deadline, int restante, int pc, int mar, EstadoProceso estado) {
        this(id, nombre, memoria, prioridad, deadline, restante, pc, mar, estado,
                TipoProceso.CPU_BOUND, MotivoBloqueo.NINGUNO, "");
    }
}
