package Modelo;

/**
 * Fila inmutable de la tabla de asignacion de archivos. etiquetaDisco es el propietario
 * que el disco guarda en cada bloque del archivo; sirve para darle el mismo color al
 * archivo en la tabla y en el disco, aunque el archivo cambie de nombre.
 * @author Luciano
 */
public record VistaAsignacionArchivo(String nombre, String ruta, int bloques, int primerBloque,
        String dueno, long procesoCreador, String etiquetaDisco) {
    public VistaAsignacionArchivo {
        if (nombre == null || nombre.isBlank() || ruta == null || !ruta.startsWith("/") || bloques < 1
                || primerBloque < 0 || dueno == null || dueno.isBlank() || procesoCreador < 0
                || etiquetaDisco == null || etiquetaDisco.isBlank())
            throw new IllegalArgumentException("Fila de asignacion invalida");
    }
}
