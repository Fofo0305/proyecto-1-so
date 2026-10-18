package Modelo;

/**
 * Atributos iniciales del proceso, sin ID ni contexto de una ejecución anterior.
 * @author Adolfo
 */
public record DefinicionProceso(String nombre, int computador, int prioridad,
        int memoria, int instrucciones, int deadline) {
    public DefinicionProceso {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (computador < 1 || prioridad < 0 || memoria < 1 || instrucciones < 1 || deadline < 1)
            throw new IllegalArgumentException("Atributos iniciales de proceso fuera de rango");
    }

    public Proceso crearProceso() {
        return new Proceso(nombre, computador, TipoProceso.CPU_BOUND, prioridad, memoria, instrucciones, deadline);
    }
}
