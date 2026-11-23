package Sistema;

import Modelo.PermisoArchivo;
import Modelo.TipoEntrada;

/** Datos mutables de un directorio o archivo, encapsulados por SistemaArchivos. @author Luciano */
final class EntradaArchivo {
    private final long id;
    private final TipoEntrada tipo;
    private final String dueno;
    private final PermisoArchivo permiso;
    private final int tamanoBloques;
    private final int primerBloque;
    private final long procesoCreador;
    private String nombre;

    /** Directorio: no ocupa bloques del disco. */
    EntradaArchivo(long id, String nombre, String dueno, PermisoArchivo permiso) {
        this(id, nombre, TipoEntrada.DIRECTORIO, dueno, permiso, 0, -1, 0);
    }

    /** Archivo: su contenido es la cadena de bloques que empieza en primerBloque. */
    EntradaArchivo(long id, String nombre, String dueno, PermisoArchivo permiso,
            int tamanoBloques, int primerBloque, long procesoCreador) {
        this(id, nombre, TipoEntrada.ARCHIVO, dueno, permiso, tamanoBloques, primerBloque, procesoCreador);
    }

    private EntradaArchivo(long id, String nombre, TipoEntrada tipo, String dueno, PermisoArchivo permiso,
            int tamanoBloques, int primerBloque, long procesoCreador) {
        if (id < 0 || tipo == null || dueno == null || dueno.isBlank() || permiso == null
                || procesoCreador < 0
                || tipo == TipoEntrada.ARCHIVO && (tamanoBloques < 1 || primerBloque < 0))
            throw new IllegalArgumentException("Entrada de archivo invalida");
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.dueno = dueno;
        this.permiso = permiso;
        this.tamanoBloques = tamanoBloques;
        this.primerBloque = primerBloque;
        this.procesoCreador = procesoCreador;
    }

    long getId() { return id; }
    String getNombre() { return nombre; }
    TipoEntrada getTipo() { return tipo; }
    String getDueno() { return dueno; }
    PermisoArchivo getPermiso() { return permiso; }
    int getTamanoBloques() { return tamanoBloques; }
    int getPrimerBloque() { return primerBloque; }
    long getProcesoCreador() { return procesoCreador; }
    /** Etiqueta estable con la que el disco marca los bloques; no cambia al renombrar. */
    String getEtiquetaDisco() { return etiquetaDisco(id); }
    static String etiquetaDisco(long id) { return "F" + id; }
    boolean esDirectorio() { return tipo == TipoEntrada.DIRECTORIO; }
    void renombrar(String nuevoNombre) { nombre = nuevoNombre; }
}
