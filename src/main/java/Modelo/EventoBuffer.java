package Modelo;

/** Evento inmutable; secuencia unica dentro del buffer. @author Luciano Rojas */
public record EventoBuffer(long secuencia, long ciclo, String buffer, int computador,
        long proceso, TipoEventoBuffer tipo, String detalle) { }
