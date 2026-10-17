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

    public int getCapacidad() { return capacidad; }
    public int getUsada() { return usada; }
    public int getLibre() { return capacidad - usada; }
}
