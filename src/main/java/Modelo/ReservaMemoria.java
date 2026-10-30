package Modelo;

/**
 * Reserva inmutable de RAM asociada a un propietario.
 * @author Adolfo
 */
public final class ReservaMemoria {
    private final String propietario;
    private final int cantidad;

    public ReservaMemoria(String propietario, int cantidad) {
        if (propietario == null || propietario.isBlank()) {
            throw new IllegalArgumentException("El propietario es obligatorio");
        }
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad reservada debe ser positiva");
        }
        this.propietario = propietario;
        this.cantidad = cantidad;
    }

    public String getPropietario() { return propietario; }
    public int getCantidad() { return cantidad; }
}
