package com.mycompany.proyecto1so2526.estructuras;

import java.util.concurrent.Semaphore;

/** Cola FIFO propia. Cada operación se protege contra accesos concurrentes.
 * @author Adolfo
 * @param <T> tipo de elemento
 */
public final class Cola<T> {
    private static final class Nodo<T> {
        final T valor;
        Nodo<T> siguiente;
        Nodo(T valor) { this.valor = valor; }
    }

    private final Semaphore mutex = new Semaphore(1, true);
    private Nodo<T> primero;
    private Nodo<T> ultimo;
    private int cantidad;

    public void encolar(T valor) {
        if (valor == null) throw new IllegalArgumentException("El elemento es obligatorio");
        mutex.acquireUninterruptibly();
        try {
            Nodo<T> nodo = new Nodo<>(valor);
            if (ultimo == null) primero = nodo;
            else ultimo.siguiente = nodo;
            ultimo = nodo;
            cantidad++;
        } finally { mutex.release(); }
    }

    public T desencolar() {
        mutex.acquireUninterruptibly();
        try {
            if (primero == null) return null;
            T valor = primero.valor;
            primero = primero.siguiente;
            if (primero == null) ultimo = null;
            cantidad--;
            return valor;
        } finally { mutex.release(); }
    }

    public int getCantidad() {
        mutex.acquireUninterruptibly();
        try { return cantidad; } finally { mutex.release(); }
    }

    /** Copia los elementos en orden sin exponer los nodos internos. */
    public Object[] instantanea() {
        mutex.acquireUninterruptibly();
        try {
            Object[] copia = new Object[cantidad];
            Nodo<T> actual = primero;
            for (int i = 0; i < cantidad; i++) {
                copia[i] = actual.valor;
                actual = actual.siguiente;
            }
            return copia;
        } finally { mutex.release(); }
    }
}
