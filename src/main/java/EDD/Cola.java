package EDD;

/**
 * Cola de elementos enlazados.
 *
 * @author Adolfo
 * @param <T> tipo del dato almacenado
 */
public class Cola<T> {
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
    }

    /**
     * Retira el primer dato de la cola.
     * @return dato retirado, o null si la cola está vacía
     */
    public T quitar() {
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
    }

    /**
     * Consulta el primer dato sin retirarlo.
     * @return primer dato, o null si la cola está vacía
     */
    public T primero() {
        return cabeza == null ? null : cabeza.getDato();
    }

    /**
     * Consulta el último dato sin retirarlo.
     * @return último dato, o null si la cola está vacía
     */
    public T ultimo() {
        return finalCol == null ? null : finalCol.getDato();
    }

    /**
     * Consulta la cantidad de elementos.
     * @return tamaño actual de la cola
     */
    public int getTamano() {
        return tamano;
    }

    /** Indica si la cola no contiene elementos. */
    public boolean estaVacia() {
        return cabeza == null;
    }
}
