package Modelo;

/** Copia del historial retenido y numero de eventos antiguos descartados. @author Luciano Rojas */
public record VistaEventosBuffer(String buffer, long descartados, EventoBuffer[] eventos) {
    public VistaEventosBuffer { eventos = eventos.clone(); }
    @Override public EventoBuffer[] eventos() { return eventos.clone(); }
}
