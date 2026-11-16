package Modelo;

/** Atributos iniciales de un proceso, sin estado de ejecución. @author Adolfo */
public record DefinicionProceso(String nombre, int computador, int prioridad,
        int memoria, int instrucciones, int deadline, TipoProceso tipo, ParametrosES parametrosES) {
    public DefinicionProceso(String nombre, int computador, int prioridad,
            int memoria, int instrucciones, int deadline) {
        this(nombre, computador, prioridad, memoria, instrucciones, deadline, TipoProceso.CPU_BOUND, null);
    }

    public DefinicionProceso {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (computador != 1 || prioridad < 0 || memoria < 1 || instrucciones < 1 || deadline < 1)
            throw new IllegalArgumentException("Atributos iniciales de proceso fuera de rango");
        if (tipo == null) throw new IllegalArgumentException("Tipo de proceso no implementado");
        if ((tipo == TipoProceso.IO_BOUND) != (parametrosES != null))
            throw new IllegalArgumentException("Parametros de E/S inconsistentes");
    }

    public Proceso crearProceso() {
        Proceso proceso = new Proceso(nombre, computador, tipo, prioridad, memoria, instrucciones, deadline);
        if (parametrosES != null) proceso.configurarES(parametrosES);
        return proceso;
    }
}
