package Modelo;

/**
 * Metadatos iniciales de un buffer; se reserva una unidad de RAM por elemento.
 * Las operaciones de producción y consumo pertenecen al módulo correspondiente.
 * @author Adolfo
 */
public record DefinicionBuffer(String nombre, int capacidad, int anfitrion) {
    public DefinicionBuffer {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre de buffer es obligatorio");
        if (capacidad < 1 || anfitrion < 1) throw new IllegalArgumentException("Capacidad y anfitrion deben ser positivos");
    }
}
