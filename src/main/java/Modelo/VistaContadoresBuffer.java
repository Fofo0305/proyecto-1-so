package Modelo;

/** Contadores y ocupacion capturados bajo el mismo mutex del buffer. @author Luciano Rojas */
public record VistaContadoresBuffer(String nombre, int anfitrion, int capacidad, int cantidad,
        long producidos, long consumidos) { }
