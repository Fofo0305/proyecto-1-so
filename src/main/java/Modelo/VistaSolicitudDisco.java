package Modelo;

/** Copia inmutable de una solicitud de la cola unica del disco. @author Adolfo */
public record VistaSolicitudDisco(long id, long procesoId, OperacionES operacion,
        int bloqueObjetivo, int bloquesTotales, int bloquesRestantes, long cicloLlegada) { }
