package EDD;

/** Nodo de un arbol jerarquico propio. @author Adolfo
 * @param <T> tipo almacenado
 */
public final class NodoArbol<T> {
    private volatile T dato;
    private NodoArbol<T> padre;
    private final ListaEnlazada<NodoArbol<T>> hijos = new ListaEnlazada<>();

    NodoArbol(T dato, NodoArbol<T> padre) {
        if (dato == null) throw new IllegalArgumentException("El dato es obligatorio");
        this.dato = dato;
        this.padre = padre;
    }

    public T getDato() { return dato; }
    public NodoArbol<T> getPadre() { return padre; }

    @SuppressWarnings("unchecked")
    public NodoArbol<T>[] getHijos() {
        Object[] elementos = hijos.instantanea();
        NodoArbol<T>[] copia = (NodoArbol<T>[]) new NodoArbol<?>[elementos.length];
        for (int i = 0; i < elementos.length; i++) copia[i] = (NodoArbol<T>) elementos[i];
        return copia;
    }

    void agregarHijo(NodoArbol<T> hijo) { hijos.agregar(hijo); }
    boolean retirarHijo(NodoArbol<T> hijo) { return hijos.eliminar(hijo); }
    void reemplazarDato(T nuevoDato) { dato = nuevoDato; }
    void desvincular() { padre = null; }
}
