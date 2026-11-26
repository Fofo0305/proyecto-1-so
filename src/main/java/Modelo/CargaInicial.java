package Modelo;

/** Configuración y procesos iniciales inmutables. @author Adolfo */
public final class CargaInicial {
    private final Configuracion configuracion;
    private final DefinicionProceso[] procesos;

    public CargaInicial(Configuracion configuracion, DefinicionProceso[] procesos) {
        if (configuracion == null || procesos == null)
            throw new IllegalArgumentException("Configuracion y procesos son obligatorios");
        int capacidad = configuracion.getRam()[0];
        for (DefinicionProceso proceso : procesos)
            if (proceso == null || proceso.computador() != 1 || proceso.memoria() > capacidad)
                throw new IllegalArgumentException("Proceso sin computador valido o con memoria superior a su RAM");
        this.configuracion = configuracion;
        this.procesos = procesos.clone();
    }

    public Configuracion getConfiguracion() { return configuracion; }
    public DefinicionProceso[] getProcesos() { return procesos.clone(); }
}
