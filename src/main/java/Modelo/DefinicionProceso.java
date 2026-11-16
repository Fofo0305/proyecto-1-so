package Modelo;

/** Atributos iniciales de un proceso, sin estado de ejecución. @author Adolfo */
public record DefinicionProceso(String nombre, int computador, int prioridad,
        int memoria, int instrucciones, int deadline, TipoProceso tipo, ParametrosES parametrosES,
        OperacionES operacionES, String rutaObjetivo) {
    public DefinicionProceso(String nombre, int computador, int prioridad,
            int memoria, int instrucciones, int deadline) {
        this(nombre, computador, prioridad, memoria, instrucciones, deadline,
                TipoProceso.CPU_BOUND, null, null, null);
    }

    public DefinicionProceso(String nombre, int computador, int prioridad, int memoria,
            int instrucciones, int deadline, TipoProceso tipo, ParametrosES parametrosES) {
        this(nombre, computador, prioridad, memoria, instrucciones, deadline,
                tipo, parametrosES, null, null);
    }

    public DefinicionProceso(String nombre, int computador, int prioridad, int memoria,
            int instrucciones, int deadline, OperacionES operacionES, String rutaObjetivo) {
        this(nombre, computador, prioridad, memoria, instrucciones, deadline,
                TipoProceso.IO_BOUND, null, operacionES, rutaObjetivo);
    }

    public DefinicionProceso {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (computador != 1 || prioridad < 0 || memoria < 1 || instrucciones < 1 || deadline < 1)
            throw new IllegalArgumentException("Atributos iniciales de proceso fuera de rango");
        if (tipo == null) throw new IllegalArgumentException("Tipo de proceso no implementado");
        boolean esTemporal = parametrosES != null;
        boolean esDisco = operacionES != null;
        if (tipo == TipoProceso.CPU_BOUND && (esTemporal || esDisco))
            throw new IllegalArgumentException("Un CPU_BOUND no puede declarar E/S");
        if (tipo == TipoProceso.IO_BOUND && esTemporal == esDisco)
            throw new IllegalArgumentException("Parametros de E/S inconsistentes");
        if (esDisco) Proceso.validarDestino(operacionES, rutaObjetivo);
        if (!esDisco && rutaObjetivo != null)
            throw new IllegalArgumentException("Una ruta requiere una operacion de disco");
    }

    public Proceso crearProceso() {
        Proceso proceso = new Proceso(nombre, computador, tipo, prioridad, memoria, instrucciones, deadline);
        if (parametrosES != null) proceso.configurarES(parametrosES);
        if (operacionES != null) proceso.configurarOperacionDisco(operacionES, rutaObjetivo);
        return proceso;
    }
}
