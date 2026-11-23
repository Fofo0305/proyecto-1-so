package Sistema;

import EDD.ArbolJerarquico;
import EDD.NodoArbol;
import Modelo.PermisoArchivo;
import Modelo.TipoEntrada;
import java.util.concurrent.Semaphore;

/** Arbol de directorios y archivos del sistema operativo, con rutas absolutas. @author Luciano */
public final class SistemaArchivos {
    public static final String SEPARADOR = "/";
    public static final String DUENO_SISTEMA = "admin";
    public static final int LARGO_MAXIMO_NOMBRE = 64;

    private final Semaphore mutex = new Semaphore(1, true);
    private final ArbolJerarquico<EntradaArchivo> arbol;
    private long siguienteId = 1;

    public SistemaArchivos() {
        arbol = new ArbolJerarquico<>(new EntradaArchivo(0, "", TipoEntrada.DIRECTORIO,
                DUENO_SISTEMA, PermisoArchivo.PUBLICO));
    }

    /** Crea un directorio vacio; su padre ya debe existir. */
    public void crearDirectorio(String ruta, String dueno, PermisoArchivo permiso) {
        mutex.acquireUninterruptibly();
        try {
            String[] partes = dividir(ruta);
            if (partes.length == 0) throw new IllegalArgumentException("La raiz ya existe");
            NodoArbol<EntradaArchivo> padre = directorioPadre(partes);
            String nombre = partes[partes.length - 1];
            if (hijo(padre, nombre) != null)
                throw new IllegalArgumentException("Ya existe " + ruta);
            arbol.agregarHijo(padre, new EntradaArchivo(siguienteId++, nombre,
                    TipoEntrada.DIRECTORIO, dueno, permiso));
        } finally { mutex.release(); }
    }

    public boolean existe(String ruta) {
        mutex.acquireUninterruptibly();
        try { return buscar(dividir(ruta)) != null; }
        finally { mutex.release(); }
    }

    public TipoEntrada tipoDe(String ruta) {
        mutex.acquireUninterruptibly();
        try { return exigir(ruta).getDato().getTipo(); }
        finally { mutex.release(); }
    }

    /** Nombres de los hijos directos de un directorio, en orden de creacion. */
    public String[] listar(String ruta) {
        mutex.acquireUninterruptibly();
        try {
            NodoArbol<EntradaArchivo> nodo = exigir(ruta);
            if (!nodo.getDato().esDirectorio())
                throw new IllegalArgumentException(ruta + " no es un directorio");
            NodoArbol<EntradaArchivo>[] hijos = nodo.getHijos();
            String[] nombres = new String[hijos.length];
            for (int i = 0; i < hijos.length; i++) nombres[i] = hijos[i].getDato().getNombre();
            return nombres;
        } finally { mutex.release(); }
    }

    /** Cantidad de entradas del arbol, incluida la raiz. */
    public int getTotalEntradas() { return arbol.getTamano(); }

    /** Normaliza una ruta absoluta: quita barras repetidas y la barra final. */
    public static String normalizar(String ruta) {
        String[] partes = dividir(ruta);
        if (partes.length == 0) return SEPARADOR;
        StringBuilder texto = new StringBuilder();
        for (String parte : partes) texto.append(SEPARADOR).append(parte);
        return texto.toString();
    }

    static String[] dividir(String ruta) {
        if (ruta == null || !ruta.startsWith(SEPARADOR))
            throw new IllegalArgumentException("La ruta debe ser absoluta: " + ruta);
        String[] crudas = ruta.split(SEPARADOR);
        int cantidad = 0;
        for (String parte : crudas) if (!parte.isEmpty()) cantidad++;
        String[] partes = new String[cantidad];
        int i = 0;
        for (String parte : crudas) {
            if (parte.isEmpty()) continue;
            validarNombre(parte);
            partes[i++] = parte;
        }
        return partes;
    }

    static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.contains(SEPARADOR)
                || nombre.equals(".") || nombre.equals("..")
                || nombre.length() > LARGO_MAXIMO_NOMBRE || !nombre.equals(nombre.strip()))
            throw new IllegalArgumentException("Nombre invalido: " + nombre);
    }

    NodoArbol<EntradaArchivo> exigir(String ruta) {
        NodoArbol<EntradaArchivo> nodo = buscar(dividir(ruta));
        if (nodo == null) throw new IllegalArgumentException("No existe " + ruta);
        return nodo;
    }

    NodoArbol<EntradaArchivo> directorioPadre(String[] partes) {
        NodoArbol<EntradaArchivo> actual = arbol.getRaiz();
        for (int i = 0; i < partes.length - 1; i++) {
            actual = hijo(actual, partes[i]);
            if (actual == null || !actual.getDato().esDirectorio())
                throw new IllegalArgumentException("El directorio padre no existe");
        }
        return actual;
    }

    private NodoArbol<EntradaArchivo> buscar(String[] partes) {
        NodoArbol<EntradaArchivo> actual = arbol.getRaiz();
        for (String parte : partes) {
            if (!actual.getDato().esDirectorio()) return null;
            actual = hijo(actual, parte);
            if (actual == null) return null;
        }
        return actual;
    }

    static NodoArbol<EntradaArchivo> hijo(NodoArbol<EntradaArchivo> padre, String nombre) {
        for (NodoArbol<EntradaArchivo> hijo : padre.getHijos())
            if (hijo.getDato().getNombre().equals(nombre)) return hijo;
        return null;
    }
}
