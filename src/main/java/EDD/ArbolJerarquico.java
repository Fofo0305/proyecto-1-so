package EDD;

import java.util.concurrent.Semaphore;

/** Arbol n-ario propio para directorios y archivos. @author Adolfo
 * @param <T> tipo almacenado
 */
public final class ArbolJerarquico<T> {
    private final Semaphore mutex = new Semaphore(1, true);
    private final NodoArbol<T> raiz;
    private int tamano = 1;

    public ArbolJerarquico(T datoRaiz) { raiz = new NodoArbol<>(datoRaiz, null); }

    public NodoArbol<T> agregarHijo(NodoArbol<T> padre, T dato) {
        if (dato == null) throw new IllegalArgumentException("El dato es obligatorio");
        mutex.acquireUninterruptibly();
        try {
            exigirPertenencia(padre);
            NodoArbol<T> hijo = new NodoArbol<>(dato, padre);
            padre.agregarHijo(hijo);
            tamano++;
            return hijo;
        } finally { mutex.release(); }
    }

    public void reemplazarDato(NodoArbol<T> nodo, T dato) {
        if (dato == null) throw new IllegalArgumentException("El dato es obligatorio");
        mutex.acquireUninterruptibly();
        try {
            exigirPertenencia(nodo);
            nodo.reemplazarDato(dato);
        } finally { mutex.release(); }
    }

    public boolean eliminarSubarbol(NodoArbol<T> nodo) {
        mutex.acquireUninterruptibly();
        try {
            exigirPertenencia(nodo);
            if (nodo == raiz) return false;
            int eliminados = contar(nodo);
            if (!nodo.getPadre().retirarHijo(nodo))
                throw new IllegalStateException("El arbol perdio la relacion con el padre");
            nodo.desvincular();
            tamano -= eliminados;
            return true;
        } finally { mutex.release(); }
    }

    public NodoArbol<T> getRaiz() { return raiz; }

    public int getTamano() {
        mutex.acquireUninterruptibly();
        try { return tamano; }
        finally { mutex.release(); }
    }

    private void exigirPertenencia(NodoArbol<T> nodo) {
        if (nodo == null) throw new IllegalArgumentException("El nodo es obligatorio");
        NodoArbol<T> ancestro = nodo;
        while (ancestro.getPadre() != null) ancestro = ancestro.getPadre();
        if (ancestro != raiz) throw new IllegalArgumentException("El nodo pertenece a otro arbol");
    }

    private int contar(NodoArbol<T> nodo) {
        int cantidad = 1;
        for (NodoArbol<T> hijo : nodo.getHijos()) cantidad += contar(hijo);
        return cantidad;
    }
}
