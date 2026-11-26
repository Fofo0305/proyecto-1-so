package Modelo;

/**
 * Lectura inmutable de un computador para la interfaz gráfica.
 * @author Adolfo
 */
public record VistaComputador(int id, long ciclo, ModoCPU modo, int usada, int libre,
        String cpu, String nuevos, String listos, String bloqueados, String terminados,
        String listosSuspendidos, String bloqueadosSuspendidos,
        VistaProceso procesoCPU, VistaProceso[] colaNuevos, VistaProceso[] colaListos,
        VistaProceso[] colaBloqueados, VistaProceso[] colaTerminados,
        VistaProceso[] colaListosSuspendidos, VistaProceso[] colaBloqueadosSuspendidos,
        PoliticaPlanificacion politica, int quantum) {
    public VistaComputador {
        colaNuevos = colaNuevos.clone(); colaListos = colaListos.clone();
        colaBloqueados = colaBloqueados.clone(); colaTerminados = colaTerminados.clone();
        colaListosSuspendidos = colaListosSuspendidos.clone();
        colaBloqueadosSuspendidos = colaBloqueadosSuspendidos.clone();
    }
    @Override public VistaProceso[] colaNuevos() { return colaNuevos.clone(); }
    @Override public VistaProceso[] colaListos() { return colaListos.clone(); }
    @Override public VistaProceso[] colaBloqueados() { return colaBloqueados.clone(); }
    @Override public VistaProceso[] colaTerminados() { return colaTerminados.clone(); }
    @Override public VistaProceso[] colaListosSuspendidos() { return colaListosSuspendidos.clone(); }
    @Override public VistaProceso[] colaBloqueadosSuspendidos() { return colaBloqueadosSuspendidos.clone(); }
}
