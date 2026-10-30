package Modelo;

/** Copias de una misma sesion. Los trabajadores pueden avanzar entre lecturas;
 * cada contador conserva su propia ocupacion atomica. @author Luciano Rojas */
public record VistaObservacionBuffers(VistaBuffer[] estados, VistaContadoresBuffer[] contadores,
        VistaBloqueosBuffer[] bloqueos, VistaBloqueoProceso[] procesos, VistaEventosBuffer[] eventos) {
    public VistaObservacionBuffers {
        estados=estados.clone(); contadores=contadores.clone(); bloqueos=bloqueos.clone();
        procesos=procesos.clone(); eventos=eventos.clone();
    }
    @Override public VistaBuffer[] estados() { return estados.clone(); }
    @Override public VistaContadoresBuffer[] contadores() { return contadores.clone(); }
    @Override public VistaBloqueosBuffer[] bloqueos() { return bloqueos.clone(); }
    @Override public VistaBloqueoProceso[] procesos() { return procesos.clone(); }
    @Override public VistaEventosBuffer[] eventos() { return eventos.clone(); }
}
