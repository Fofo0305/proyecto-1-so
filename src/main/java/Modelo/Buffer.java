package Modelo;

import EDD.Cola;
import java.util.concurrent.Semaphore;

/**
 * Almacenamiento FIFO de capacidad limitada de un computador anfitrion.
 * El mutex protege operaciones reales entre hilos; no representa los semaforos
 * simulados que bloquearan a los procesos productores y consumidores.
 * @author Luciano Rojas
 * @param <T> tipo de elemento almacenado
 */
public final class Buffer<T> {
    private final DefinicionBuffer definicion;
    private final Cola<T> elementos = new Cola<>();
    private final Semaphore mutex = new Semaphore(1, true);

    public Buffer(String nombre, int capacidad, int anfitrion) {
        this.definicion = new DefinicionBuffer(nombre, capacidad, anfitrion);
    }

    /**
     * Intenta insertar un elemento sin esperar espacio disponible.
     * @return false si el buffer esta lleno, sin modificar su contenido
     */
    public boolean insertar(T elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("El elemento no puede ser null");
        }
        mutex.acquireUninterruptibly();
        try {
            if (elementos.getTamano() == getCapacidad()) return false;
            elementos.agregar(elemento);
            return true;
        } finally {
            mutex.release();
        }
    }

    /** Retira el elemento mas antiguo, o devuelve null si esta vacio. */
    public T extraer() {
        mutex.acquireUninterruptibly();
        try {
            return elementos.quitar();
        } finally {
            mutex.release();
        }
    }

    /** Copia el contenido en orden FIFO sin exponer la cola interna. */
    public Object[] instantanea() {
        mutex.acquireUninterruptibly();
        try {
            return elementos.instantanea();
        } finally {
            mutex.release();
        }
    }

    public int getCantidad() {
        mutex.acquireUninterruptibly();
        try {
            return elementos.getTamano();
        } finally {
            mutex.release();
        }
    }

    public boolean estaVacio() { return getCantidad() == 0; }
    public boolean estaLleno() { return getCantidad() == getCapacidad(); }
    public String getNombre() { return definicion.nombre(); }
    public int getCapacidad() { return definicion.capacidad(); }
    public int getAnfitrion() { return definicion.anfitrion(); }
}
