package Modelo;

/**
 * Capacidad y ocupación de la RAM de un computador.
 * @author Adolfo
 */
public class Memoria {
    private final int capacidad;
    private int usada;

    public Memoria(int capacidad) {
        if (capacidad < 1) {
            throw new IllegalArgumentException("La capacidad de RAM debe ser positiva");
        }
        this.capacidad = capacidad;
        this.usada = 0;
    }

    /**
     * Reserva unidades de RAM si hay espacio suficiente.
     * @param cantidad unidades solicitadas
     * @return true si se realizó la reserva; false si falta espacio
     */
    public boolean reservar(int cantidad) {
        if (cantidad < 1) {
            throw new IllegalArgumentException("La reserva debe ser positiva");
        }
        if (cantidad > capacidad - usada) {
            return false;
        }
        usada += cantidad;
        return true;
    }

    public int getCapacidad() { return capacidad; }
    public int getUsada() { return usada; }
    public int getLibre() { return capacidad - usada; }
}
