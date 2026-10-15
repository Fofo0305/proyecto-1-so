package EDD;

import java.util.concurrent.Semaphore;

/**
 * Cola de elementos enlazados.
 *
 * @author Adolfo
 * @param <T> tipo del dato almacenado
 */
public class Cola<T> {
    private final Semaphore mutex = new Semaphore(1, true);
    private Nodo<T> cabeza;
    private Nodo<T> finalCol;
    private int tamano;

    public Cola() {
        this.cabeza = null;
        this.finalCol = null;
        this.tamano = 0;
    }

    /**
     * Agrega un dato al final de la cola.
     * @param dato elemento que se agrega
     * @throws IllegalArgumentException si el dato es null
     */
    public void agregar(T dato) {
        mutex.acquireUninterruptibly();
        try {
            if (dato == null) {
                throw new IllegalArgumentException("El dato no puede ser null");
            }
            Nodo<T> nuevo = new Nodo<>(dato);
            if (finalCol == null) {
                cabeza = nuevo;
            } else {
                finalCol.setSiguiente(nuevo);
            }
            finalCol = nuevo;
            tamano++;
        } finally {
            mutex.release();
        }
    }

    /**
     * Retira el primer dato de la cola.
     * @return dato retirado, o null si la cola está vacía
     */
    public T quitar() {
        mutex.acquireUninterruptibly();
        try {
            if (cabeza == null) {
                return null;
            }
            T dato = cabeza.getDato();
            cabeza = cabeza.getSiguiente();
            if (cabeza == null) {
                finalCol = null;
            }
            tamano--;
            return dato;
        } finally {
            mutex.release();
        }
    }

    /**
     * Consulta el primer dato sin retirarlo.
     * @return primer dato, o null si la cola está vacía
     */
    public T primero() {
        mutex.acquireUninterruptibly();
        try {
            return cabeza == null ? null : cabeza.getDato();
        } finally {
            mutex.release();
        }
    }

    /**
     * Consulta el último dato sin retirarlo.
     * @return último dato, o null si la cola está vacía
     */
    public T ultimo() {
        mutex.acquireUninterruptibly();
        try {
            return finalCol == null ? null : finalCol.getDato();
        } finally {
            mutex.release();
        }
    }

    /**
     * Consulta la cantidad de elementos.
     * @return tamaño actual de la cola
     */
    public int getTamano() {
        mutex.acquireUninterruptibly();
        try {
            return tamano;
        } finally {
            mutex.release();
        }
    }

    /** Indica si la cola no contiene elementos. */
    public boolean estaVacia() {
        mutex.acquireUninterruptibly();
        try {
            return cabeza == null;
        } finally {
            mutex.release();
        }
    }
}
