package Modelo;

import java.util.concurrent.Semaphore;
import EDD.Cola;

/**
 * Capacidad y ocupación de la RAM de un computador.
 * @author Adolfo
 */
public class Memoria {
    private final Semaphore mutex = new Semaphore(1, true);
    private final int capacidad;
    private int usada;
    private int sinPropietario;
    private final Cola<ReservaMemoria> reservas = new Cola<>();

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
            sinPropietario += cantidad;
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
            if (cantidad < 1 || cantidad > sinPropietario) {
                throw new IllegalArgumentException("La liberacion no puede superar la RAM reservada sin propietario");
            }
            usada -= cantidad;
            sinPropietario -= cantidad;
        } finally {
            mutex.release();
        }
    }

    /**
     * Reserva RAM para un propietario único, por ejemplo proceso:ID o buffer:ID.
     * @param propietario identificador del propietario
     * @param cantidad unidades solicitadas
     * @return false si no hay espacio; true si se registró la reserva
     */
    public boolean reservar(String propietario, int cantidad) {
        ReservaMemoria nueva = new ReservaMemoria(propietario, cantidad);
        mutex.acquireUninterruptibly();
        try {
            for (Object elemento : reservas.instantanea()) {
                ReservaMemoria reserva = (ReservaMemoria) elemento;
                if (reserva.getPropietario().equals(propietario)) {
                    throw new IllegalArgumentException("El propietario ya tiene una reserva");
                }
            }
            if (cantidad > capacidad - usada) return false;
            reservas.agregar(nueva);
            usada += cantidad;
            return true;
        } finally {
            mutex.release();
        }
    }

    /**
     * Libera la reserva completa de un propietario sin afectar otras reservas.
     * @param propietario identificador del propietario
     * @return unidades liberadas, o cero si no existe la reserva
     */
    public int liberar(String propietario) {
        if (propietario == null || propietario.isBlank()) {
            throw new IllegalArgumentException("El propietario es obligatorio");
        }
        mutex.acquireUninterruptibly();
        try {
            int cantidad = 0;
            int total = reservas.getTamano();
            for (int i = 0; i < total; i++) {
                ReservaMemoria reserva = reservas.quitar();
                if (reserva.getPropietario().equals(propietario)) {
                    cantidad = reserva.getCantidad();
                } else {
                    reservas.agregar(reserva);
                }
            }
            usada -= cantidad;
            return cantidad;
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
