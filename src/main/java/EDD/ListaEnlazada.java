package EDD;

import java.util.Objects;
import java.util.concurrent.Semaphore;

/** Lista simple propia, protegida para las estructuras compartidas. @author Adolfo
 * @param <T> tipo almacenado
 */
public final class ListaEnlazada<T> {
    private final Semaphore mutex = new Semaphore(1, true);
    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    public void agregar(T dato) {
        if (dato == null) throw new IllegalArgumentException("El dato es obligatorio");
        mutex.acquireUninterruptibly();
        try {
            Nodo<T> nuevo = new Nodo<>(dato);
            if (cola == null) cabeza = nuevo;
            else cola.setSiguiente(nuevo);
            cola = nuevo;
            tamano++;
        } finally { mutex.release(); }
    }

    public T obtener(int indice) {
        mutex.acquireUninterruptibly();
        try { return nodoEn(indice).getDato(); }
        finally { mutex.release(); }
    }

    public boolean eliminar(T dato) {
        mutex.acquireUninterruptibly();
        try {
            Nodo<T> anterior = null;
            Nodo<T> actual = cabeza;
            while (actual != null && !Objects.equals(actual.getDato(), dato)) {
                anterior = actual;
                actual = actual.getSiguiente();
            }
            if (actual == null) return false;
            if (anterior == null) cabeza = actual.getSiguiente();
            else anterior.setSiguiente(actual.getSiguiente());
            if (cola == actual) cola = anterior;
            tamano--;
            return true;
        } finally { mutex.release(); }
    }

    public boolean contiene(T dato) {
        mutex.acquireUninterruptibly();
        try {
            Nodo<T> actual = cabeza;
            while (actual != null) {
                if (Objects.equals(actual.getDato(), dato)) return true;
                actual = actual.getSiguiente();
            }
            return false;
        } finally { mutex.release(); }
    }

    public Object[] instantanea() {
        mutex.acquireUninterruptibly();
        try {
            Object[] copia = new Object[tamano];
            Nodo<T> actual = cabeza;
            for (int i = 0; i < copia.length; i++) {
                copia[i] = actual.getDato();
                actual = actual.getSiguiente();
            }
            return copia;
        } finally { mutex.release(); }
    }

    public int getTamano() {
        mutex.acquireUninterruptibly();
        try { return tamano; }
        finally { mutex.release(); }
    }

    public boolean estaVacia() { return getTamano() == 0; }

    private Nodo<T> nodoEn(int indice) {
        if (indice < 0 || indice >= tamano) throw new IndexOutOfBoundsException("Indice: " + indice);
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) actual = actual.getSiguiente();
        return actual;
    }
}
