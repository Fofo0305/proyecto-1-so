package Modelo;

/** Lectura inmutable capturada bajo el bloqueo del anfitrion. @author Luciano Rojas */
public record VistaBuffer(String nombre, int anfitrion, int capacidad, int cantidad, String contenido,
        int mutex, int vacios, int llenos, String esperaMutex, String esperaVacios, String esperaLlenos) { }
