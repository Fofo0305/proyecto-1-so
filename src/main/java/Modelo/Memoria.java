package Modelo;

import java.util.concurrent.Semaphore;

/**
 * Capacidad y ocupación de la RAM de un computador.
 * @author Adolfo
 */
public class Memoria {
    private final Semaphore mutex = new Semaphore(1, true);
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
        mutex.acquireUninterruptibly();
        try {
            if (cantidad < 1) {
                throw new IllegalArgumentException("La reserva debe ser positiva");
            }
            if (cantidad > capacidad - usada) {
                return false;
            }
            usada += cantidad;
            return true;
        } finally {
            mutex.release();
        }
    }

    /**
     * Devuelve unidades previamente reservadas a la RAM disponible.
     * @param cantidad unidades que se liberan
     */
    public void liberar(int cantidad) {
        mutex.acquireUninterruptibly();
        try {
            if (cantidad < 1 || cantidad > usada) {
                throw new IllegalArgumentException("La liberacion debe ser positiva y no superar la RAM reservada");
            }
            usada -= cantidad;
        } finally {
            mutex.release();
        }
    }

    public int getCapacidad() { return capacidad; }
    public int getUsada() {
        mutex.acquireUninterruptibly();
        try { return usada; } finally { mutex.release(); }
    }

    public int getLibre() {
        mutex.acquireUninterruptibly();
        try { return capacidad - usada; } finally { mutex.release(); }
    }
}
