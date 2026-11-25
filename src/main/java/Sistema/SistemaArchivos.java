package Sistema;

import EDD.ArbolJerarquico;
import EDD.NodoArbol;
import Modelo.ModoUsuario;
import Modelo.PermisoArchivo;
import Modelo.TipoEntrada;
import Modelo.VistaAsignacionArchivo;
import Modelo.VistaEntradaArchivo;
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
    private ModoUsuario modo = ModoUsuario.ADMINISTRADOR;
    private String usuario = DUENO_SISTEMA;

    public SistemaArchivos(Disco disco) {
        if (disco == null) throw new IllegalArgumentException("El disco es obligatorio");
        this.disco = disco;
        arbol = new ArbolJerarquico<>(new EntradaArchivo(0, "", DUENO_SISTEMA, PermisoArchivo.PUBLICO));
    }

    /**
     * Cambia quien opera el sistema de archivos. El administrador siempre actua como
     * {@value #DUENO_SISTEMA}; un usuario necesita un nombre propio.
     */
    public void cambiarSesion(ModoUsuario nuevoModo, String nuevoUsuario) {
        if (nuevoModo == null) throw new IllegalArgumentException("El modo es obligatorio");
        String nombre = nuevoModo == ModoUsuario.ADMINISTRADOR ? DUENO_SISTEMA : nuevoUsuario;
        validarDueno(nombre);
        if (nuevoModo == ModoUsuario.USUARIO && nombre.equals(DUENO_SISTEMA))
            throw new IllegalArgumentException("El modo usuario no puede actuar como " + DUENO_SISTEMA);
        mutex.acquireUninterruptibly();
        try {
            modo = nuevoModo;
            usuario = nombre;
        } finally { mutex.release(); }
    }

    public ModoUsuario getModo() {
        mutex.acquireUninterruptibly();
        try { return modo; }
        finally { mutex.release(); }
    }

    public String getUsuario() {
        mutex.acquireUninterruptibly();
        try { return usuario; }
        finally { mutex.release(); }
    }

    /** Crea un directorio vacio; su padre ya debe existir. Solo el administrador crea directorios. */
    public void crearDirectorio(String ruta, String dueno, PermisoArchivo permiso) {
        validarDueno(dueno);
        if (permiso == null) throw new IllegalArgumentException("El permiso es obligatorio");
        mutex.acquireUninterruptibly();
        try {
            String[] partes = dividir(ruta);
            if (partes.length == 0) throw new IllegalArgumentException("La raiz ya existe");
            exigirAdministrador("crear directorios");
            NodoArbol<EntradaArchivo> padre = padreLibre(partes, ruta);
            arbol.agregarHijo(padre, new EntradaArchivo(siguienteId++,
                    partes[partes.length - 1], dueno, permiso));
        } finally { mutex.release(); }
    }

    /**
     * Crea un archivo y le asigna sus bloques encadenados en el disco.
     * Si no hay espacio, el arbol queda igual. Un usuario solo crea archivos a su nombre
     * y dentro de un directorio suyo.
     * @return numero del primer bloque asignado
     */
    public int crearArchivo(String ruta, String dueno, int tamanoBloques,
            PermisoArchivo permiso, long procesoCreador) {
        return crearArchivo(ruta, dueno, tamanoBloques, permiso, procesoCreador, -1, null);
    }

    /**
     * Restaura un archivo guardado cuya cadena empieza en {@code primerBloque}. Solo el
     * administrador restaura, porque el archivo conserva el dueno que traia.
     * @return numero del primer bloque asignado
     */
    public int restaurarArchivo(String ruta, String dueno, int tamanoBloques,
            PermisoArchivo permiso, long procesoCreador, int primerBloque) {
        if (primerBloque < 0) throw new IllegalArgumentException("El primer bloque es invalido");
        return crearArchivo(ruta, dueno, tamanoBloques, permiso, procesoCreador, primerBloque, null);
    }

    /**
     * Restaura un archivo guardado con los bloques exactos de su cadena, en orden de enlace.
     * Asi un archivo fragmentado vuelve a ocupar los mismos bloques que tenia.
     * @return numero del primer bloque asignado
     */
    public int restaurarArchivo(String ruta, String dueno, PermisoArchivo permiso,
            long procesoCreador, int[] cadena) {
        if (cadena == null || cadena.length == 0) throw new IllegalArgumentException("La cadena es obligatoria");
        int[] copia = cadena.clone();
        return crearArchivo(ruta, dueno, copia.length, permiso, procesoCreador, copia[0], copia);
    }

    /**
     * Con {@code primerPedido} igual a -1 asigna por primer ajuste; con {@code cadena} usa
     * exactamente esos bloques.
     */
    private int crearArchivo(String ruta, String dueno, int tamanoBloques,
            PermisoArchivo permiso, long procesoCreador, int primerPedido, int[] cadena) {
        validarDueno(dueno);
        if (permiso == null) throw new IllegalArgumentException("El permiso es obligatorio");
        if (procesoCreador < 0) throw new IllegalArgumentException("El proceso creador es invalido");
        mutex.acquireUninterruptibly();
        try {
            String[] partes = dividir(ruta);
            if (partes.length == 0) throw new IllegalArgumentException("La raiz no es un archivo");
            NodoArbol<EntradaArchivo> padre = padreLibre(partes, ruta);
            if (tamanoBloques < 1) throw new IllegalArgumentException("El archivo necesita al menos un bloque");
            if (modo == ModoUsuario.USUARIO) {
                if (!dueno.equals(usuario))
                    throw new SecurityException(usuario + " no puede crear archivos a nombre de " + dueno);
                if (!padre.getDato().getDueno().equals(usuario))
                    throw new SecurityException(usuario + " solo puede crear archivos en sus propios directorios");
            }
            if (primerPedido >= 0 || cadena != null) exigirAdministrador("restaurar archivos guardados");
            long id = siguienteId;
            String etiqueta = EntradaArchivo.etiquetaDisco(id);
            int primero = cadena != null ? disco.asignarCadena(etiqueta, cadena)
                    : primerPedido < 0 ? disco.asignarEncadenado(etiqueta, tamanoBloques)
                    : disco.asignarDesde(etiqueta, primerPedido, tamanoBloques);
            siguienteId++;
            arbol.agregarHijo(padre, new EntradaArchivo(id, partes[partes.length - 1], dueno,
                    permiso, tamanoBloques, primero, procesoCreador));
            return primero;
        } finally { mutex.release(); }
    }

    /**
     * Lee un archivo: devuelve sus bloques en el orden en que el disco los encadena.
     * Un usuario solo lee sus archivos o los publicos.
     */
    public int[] leer(String ruta) {
        mutex.acquireUninterruptibly();
        try {
            EntradaArchivo entrada = exigir(ruta).getDato();
            if (entrada.esDirectorio()) throw new IllegalArgumentException(ruta + " es un directorio");
            if (!puedeLeer(entrada))
                throw new SecurityException(ruta + " es privado de " + entrada.getDueno());
            return disco.recorrerCadena(entrada.getPrimerBloque());
        } finally { mutex.release(); }
    }

    /** Indica si la sesion actual puede leer la ruta, sin lanzar excepciones de permiso. */
    public boolean puedeLeer(String ruta) {
        mutex.acquireUninterruptibly();
        try { return puedeLeer(exigir(ruta).getDato()); }
        finally { mutex.release(); }
    }

    private boolean puedeLeer(EntradaArchivo entrada) {
        return modo == ModoUsuario.ADMINISTRADOR || entrada.getPermiso() == PermisoArchivo.PUBLICO
                || entrada.getDueno().equals(usuario);
    }

    /** Cambia el nombre de un archivo o directorio sin moverlo ni tocar sus bloques. Solo administrador. */
    public void renombrar(String ruta, String nuevoNombre) {
        validarNombre(nuevoNombre);
        mutex.acquireUninterruptibly();
        try {
            NodoArbol<EntradaArchivo> nodo = exigir(ruta);
            if (nodo.getPadre() == null) throw new IllegalArgumentException("La raiz no se renombra");
            exigirAdministrador("renombrar");
            NodoArbol<EntradaArchivo> existente = hijo(nodo.getPadre(), nuevoNombre);
            if (existente != null && existente != nodo)
                throw new IllegalArgumentException("Ya existe " + nuevoNombre + " en ese directorio");
            nodo.getDato().renombrar(nuevoNombre);
        } finally { mutex.release(); }
    }

    /**
     * Elimina un archivo o un directorio con todo su contenido. Primero comprueba que
     * todas las cadenas del subarbol estan sanas y solo entonces libera, para no dejar
     * el arbol a medio borrar. Un usuario solo elimina archivos propios, nunca directorios.
     * @return bloques liberados en el disco
     */
    public int eliminar(String ruta) {
        mutex.acquireUninterruptibly();
        try {
            NodoArbol<EntradaArchivo> nodo = exigir(ruta);
            if (nodo.getPadre() == null) throw new IllegalArgumentException("La raiz no se elimina");
            EntradaArchivo entrada = nodo.getDato();
            if (modo == ModoUsuario.USUARIO) {
                if (entrada.esDirectorio())
                    throw new SecurityException("Solo el administrador elimina directorios");
                if (!entrada.getDueno().equals(usuario))
                    throw new SecurityException(usuario + " no puede eliminar archivos de " + entrada.getDueno());
            }
            int[] primeros = new int[contarArchivos(nodo)];
            recolectarPrimerosBloques(nodo, primeros, 0);
            for (int primero : primeros) disco.recorrerCadena(primero);
            int liberados = 0;
            for (int primero : primeros) liberados += disco.liberarEncadenado(primero);
            arbol.eliminarSubarbol(nodo);
            return liberados;
        } finally { mutex.release(); }
    }

    private static int contarArchivos(NodoArbol<EntradaArchivo> nodo) {
        if (!nodo.getDato().esDirectorio()) return 1;
        int total = 0;
        for (NodoArbol<EntradaArchivo> hijo : nodo.getHijos()) total += contarArchivos(hijo);
        return total;
    }

    /** Recorre el subarbol en profundidad y guarda el primer bloque de cada archivo. */
    private static int recolectarPrimerosBloques(NodoArbol<EntradaArchivo> nodo, int[] destino, int indice) {
        if (!nodo.getDato().esDirectorio()) {
            destino[indice] = nodo.getDato().getPrimerBloque();
            return indice + 1;
        }
        for (NodoArbol<EntradaArchivo> hijo : nodo.getHijos())
            indice = recolectarPrimerosBloques(hijo, destino, indice);
        return indice;
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

    /**
     * Copia inmutable de todo el arbol para el JTree. Todos los modos ven la estructura
     * completa; los permisos se aplican al leer.
     */
    public VistaEntradaArchivo consultarArbol() {
        mutex.acquireUninterruptibly();
        try { return vista(arbol.getRaiz(), SEPARADOR); }
        finally { mutex.release(); }
    }

    private static VistaEntradaArchivo vista(NodoArbol<EntradaArchivo> nodo, String ruta) {
        EntradaArchivo entrada = nodo.getDato();
        NodoArbol<EntradaArchivo>[] hijos = nodo.getHijos();
        VistaEntradaArchivo[] vistas = new VistaEntradaArchivo[hijos.length];
        for (int i = 0; i < hijos.length; i++)
            vistas[i] = vista(hijos[i], rutaHija(ruta, hijos[i].getDato().getNombre()));
        return new VistaEntradaArchivo(ruta, entrada.getNombre(), entrada.getTipo(),
                entrada.getTamanoBloques(), entrada.getDueno(), entrada.getPermiso(),
                entrada.getPrimerBloque(), vistas);
    }

    /** Filas de la tabla de asignacion: un archivo por fila, en el orden del arbol. */
    public VistaAsignacionArchivo[] consultarAsignacion() {
        mutex.acquireUninterruptibly();
        try {
            VistaAsignacionArchivo[] filas = new VistaAsignacionArchivo[contarArchivos(arbol.getRaiz())];
            recolectarFilas(arbol.getRaiz(), SEPARADOR, filas, 0);
            return filas;
        } finally { mutex.release(); }
    }

    private static int recolectarFilas(NodoArbol<EntradaArchivo> nodo, String ruta,
            VistaAsignacionArchivo[] destino, int indice) {
        EntradaArchivo entrada = nodo.getDato();
        if (!entrada.esDirectorio()) {
            destino[indice] = new VistaAsignacionArchivo(entrada.getNombre(), ruta, entrada.getTamanoBloques(),
                    entrada.getPrimerBloque(), entrada.getDueno(), entrada.getProcesoCreador(),
                    entrada.getEtiquetaDisco());
            return indice + 1;
        }
        for (NodoArbol<EntradaArchivo> hijo : nodo.getHijos())
            indice = recolectarFilas(hijo, rutaHija(ruta, hijo.getDato().getNombre()), destino, indice);
        return indice;
    }

    private static String rutaHija(String ruta, String nombre) {
        return ruta.equals(SEPARADOR) ? SEPARADOR + nombre : ruta + SEPARADOR + nombre;
    }

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

    static void validarDueno(String dueno) {
        if (dueno == null || dueno.isBlank() || !dueno.equals(dueno.strip()) || dueno.contains(SEPARADOR))
            throw new IllegalArgumentException("Dueno invalido: " + dueno);
    }

    /** Rechaza la operacion si la sesion no es de administrador, explicando el motivo. */
    private void exigirAdministrador(String operacion) {
        if (modo != ModoUsuario.ADMINISTRADOR)
            throw new SecurityException("Solo el administrador puede " + operacion + "; " + usuario
                    + " esta en modo usuario");
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
