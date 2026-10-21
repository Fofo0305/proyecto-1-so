package Modelo;

/**
 * Atributos iniciales del proceso, sin ID ni contexto de una ejecución anterior.
 * @author Adolfo
 */
public record DefinicionProceso(String nombre, int computador, int prioridad,
        int memoria, int instrucciones, int deadline, TipoProceso tipo, ParametrosBuffer parametrosBuffer) {
    public DefinicionProceso(String nombre, int computador, int prioridad,
            int memoria, int instrucciones, int deadline) {
        this(nombre, computador, prioridad, memoria, instrucciones, deadline, TipoProceso.CPU_BOUND, null);
    }

    public DefinicionProceso {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (computador < 1 || prioridad < 0 || memoria < 1 || instrucciones < 1 || deadline < 1)
            throw new IllegalArgumentException("Atributos iniciales de proceso fuera de rango");
        if (tipo == null || tipo == TipoProceso.IO_BOUND)
            throw new IllegalArgumentException("Tipo de proceso no implementado");
        boolean comunicante = tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR;
        if (comunicante != (parametrosBuffer != null)
                || (comunicante && instrucciones != parametrosBuffer.instrucciones()))
            throw new IllegalArgumentException("Los parametros del buffer no corresponden al proceso");
    }

    public Proceso crearProceso() {
        return crearProceso(null);
    }

    public Proceso crearProceso(Buffer<Object> buffer) {
        return new Proceso(nombre, computador, tipo, prioridad, memoria, instrucciones, deadline, parametrosBuffer, buffer);
    }
}
