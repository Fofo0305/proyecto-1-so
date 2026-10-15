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

    /** Indica si la cola no contiene elementos. */
    public boolean estaVacia() {
        return cabeza == null;
    }
}
