package Modelo;

/**
 * Foto del disco y del sistema de archivos para la interfaz: se arma en un solo llamado al
 * controlador y despues la interfaz la dibuja sin tomar ningun semaforo.
 * activa es null cuando el disco esta libre. rechazos son los ultimos eventos
 * RECHAZO_ARCHIVO del computador, del mas viejo al mas nuevo.
 * @author Luciano
 */
public record VistaAlmacenamiento(long ciclo, PoliticaDisco politica, int cabezal,
        DireccionDisco direccion, int tamanoSwap, VistaBloqueDisco[] bloques,
        VistaSolicitudDisco activa, VistaSolicitudDisco[] cola, VistaMetricasDisco metricas,
        EventoSistema[] eventosDisco, VistaEntradaArchivo arbol, VistaAsignacionArchivo[] asignacion,
        ModoUsuario modo, String usuario, EventoSistema[] rechazos) {
    public VistaAlmacenamiento {
        if (politica == null || direccion == null || bloques == null || cola == null || metricas == null
                || eventosDisco == null || arbol == null || asignacion == null || modo == null
                || usuario == null || rechazos == null)
            throw new IllegalArgumentException("Vista de almacenamiento incompleta");
        bloques = bloques.clone();
        cola = cola.clone();
        eventosDisco = eventosDisco.clone();
        asignacion = asignacion.clone();
        rechazos = rechazos.clone();
    }
    @Override public VistaBloqueDisco[] bloques() { return bloques.clone(); }
    @Override public VistaSolicitudDisco[] cola() { return cola.clone(); }
    @Override public EventoSistema[] eventosDisco() { return eventosDisco.clone(); }
    @Override public VistaAsignacionArchivo[] asignacion() { return asignacion.clone(); }
    @Override public EventoSistema[] rechazos() { return rechazos.clone(); }
}
