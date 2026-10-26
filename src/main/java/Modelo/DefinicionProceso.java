package Modelo;

/**
 * Atributos iniciales del proceso, sin ID ni contexto de una ejecución anterior.
 * @author Adolfo
 */
public record DefinicionProceso(String nombre, int computador, int prioridad,
        int memoria, int instrucciones, int deadline, TipoProceso tipo, ParametrosBuffer parametrosBuffer, ParametrosES parametrosES) {
    public DefinicionProceso(String n,int c,int p,int m,int i,int d,TipoProceso t,ParametrosBuffer b) { this(n,c,p,m,i,d,t,b,t==TipoProceso.IO_BOUND?new ParametrosES(2,3):null); }
    public DefinicionProceso(String nombre, int computador, int prioridad,
            int memoria, int instrucciones, int deadline) {
        this(nombre, computador, prioridad, memoria, instrucciones, deadline, TipoProceso.CPU_BOUND, null);
    }

    public DefinicionProceso {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (computador < 1 || prioridad < 0 || memoria < 1 || instrucciones < 1 || deadline < 1)
            throw new IllegalArgumentException("Atributos iniciales de proceso fuera de rango");
        if (tipo == null)
            throw new IllegalArgumentException("Tipo de proceso no implementado");
        if ((tipo == TipoProceso.IO_BOUND) != (parametrosES != null)) throw new IllegalArgumentException("Parametros de E/S inconsistentes");
        boolean comunicante = tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR;
        if (comunicante != (parametrosBuffer != null)
                || (comunicante && instrucciones != parametrosBuffer.instrucciones()))
            throw new IllegalArgumentException("Los parametros del buffer no corresponden al proceso");
    }

    public Proceso crearProceso() {
        return crearProceso(null);
    }

    public Proceso crearProceso(Buffer<Object> buffer) {
        Proceso proceso = new Proceso(nombre, computador, tipo, prioridad, memoria, instrucciones, deadline, parametrosBuffer, buffer);
        if (parametrosES != null) proceso.configurarES(parametrosES);
        return proceso;
    }
}
