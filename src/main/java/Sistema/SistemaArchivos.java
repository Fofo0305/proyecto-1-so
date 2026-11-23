package Sistema;

import EDD.ArbolJerarquico;
import EDD.NodoArbol;
import Modelo.PermisoArchivo;
import Modelo.TipoEntrada;
import java.util.concurrent.Semaphore;

/**
 * Arbol de directorios y archivos del sistema operativo, con rutas absolutas.
 * Los archivos guardan solo su estructura: el contenido es la cadena de bloques en el Disco.
 * @author Luciano
 */
public final class SistemaArchivos {
    public static final String SEPARADOR = "/";
    public static final String DUENO_SISTEMA = "admin";
    public static final int LARGO_MAXIMO_NOMBRE = 64;

    private final Semaphore mutex = new Semaphore(1, true);
    private final ArbolJerarquico<EntradaArchivo> arbol;
    private final Disco disco;
    private long siguienteId = 1;

    public SistemaArchivos(Disco disco) {
        if (disco == null) throw new IllegalArgumentException("El disco es obligatorio");
        this.disco = disco;
        arbol = new ArbolJerarquico<>(new EntradaArchivo(0, "", DUENO_SISTEMA, PermisoArchivo.PUBLICO));
    }

    /** Crea un directorio vacio; su padre ya debe existir. */
    public void crearDirectorio(String ruta, String dueno, PermisoArchivo permiso) {
        mutex.acquireUninterruptibly();
        try {
            String[] partes = dividir(ruta);
            if (partes.length == 0) throw new IllegalArgumentException("La raiz ya existe");
            NodoArbol<EntradaArchivo> padre = padreLibre(partes, ruta);
            arbol.agregarHijo(padre, new EntradaArchivo(siguienteId++,
                    partes[partes.length - 1], dueno, permiso));
        } finally { mutex.release(); }
    }

    /**
     * Crea un archivo y le asigna sus bloques encadenados en el disco.
     * Si no hay espacio, el arbol queda igual.
     * @return numero del primer bloque asignado
     */
    public int crearArchivo(String ruta, String dueno, int tamanoBloques,
            PermisoArchivo permiso, long procesoCreador) {
        mutex.acquireUninterruptibly();
        try {
            String[] partes = dividir(ruta);
            if (partes.length == 0) throw new IllegalArgumentException("La raiz no es un archivo");
            NodoArbol<EntradaArchivo> padre = padreLibre(partes, ruta);
            if (tamanoBloques < 1) throw new IllegalArgumentException("El archivo necesita al menos un bloque");
            long id = siguienteId;
            int primero = disco.asignarEncadenado(EntradaArchivo.etiquetaDisco(id), tamanoBloques);
            siguienteId++;
            arbol.agregarHijo(padre, new EntradaArchivo(id, partes[partes.length - 1], dueno,
                    permiso, tamanoBloques, primero, procesoCreador));
            return primero;
        } finally { mutex.release(); }
    }

    /** Lee un archivo: devuelve sus bloques en el orden en que el disco los encadena. */
    public int[] leer(String ruta) {
        mutex.acquireUninterruptibly();
        try {
            EntradaArchivo entrada = exigir(ruta).getDato();
            if (entrada.esDirectorio()) throw new IllegalArgumentException(ruta + " es un directorio");
            return disco.recorrerCadena(entrada.getPrimerBloque());
        } finally { mutex.release(); }
    }

    /** Cambia el nombre de un archivo o directorio sin moverlo ni tocar sus bloques. */
    public void renombrar(String ruta, String nuevoNombre) {
        validarNombre(nuevoNombre);
        mutex.acquireUninterruptibly();
        try {
            NodoArbol<EntradaArchivo> nodo = exigir(ruta);
            if (nodo.getPadre() == null) throw new IllegalArgumentException("La raiz no se renombra");
            NodoArbol<EntradaArchivo> existente = hijo(nodo.getPadre(), nuevoNombre);
            if (existente != null && existente != nodo)
                throw new IllegalArgumentException("Ya existe " + nuevoNombre + " en ese directorio");
            nodo.getDato().renombrar(nuevoNombre);
        } finally { mutex.release(); }
    }

    /** Elimina un archivo, liberando sus bloques, o un directorio vacio. */
    public void eliminar(String ruta) {
        mutex.acquireUninterruptibly();
        try {
            NodoArbol<EntradaArchivo> nodo = exigir(ruta);
            if (nodo.getPadre() == null) throw new IllegalArgumentException("La raiz no se elimina");
            EntradaArchivo entrada = nodo.getDato();
            if (entrada.esDirectorio() && nodo.getHijos().length > 0)
                throw new IllegalStateException(ruta + " no esta vacio");
            if (!entrada.esDirectorio()) disco.liberarEncadenado(entrada.getPrimerBloque());
            arbol.eliminarSubarbol(nodo);
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

    private NodoArbol<EntradaArchivo> padreLibre(String[] partes, String ruta) {
        NodoArbol<EntradaArchivo> padre = directorioPadre(partes);
        if (hijo(padre, partes[partes.length - 1]) != null)
            throw new IllegalArgumentException("Ya existe " + ruta);
        return padre;
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
