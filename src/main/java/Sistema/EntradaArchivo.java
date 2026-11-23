package Sistema;

import Modelo.PermisoArchivo;
import Modelo.TipoEntrada;

/** Datos mutables de un directorio o archivo, encapsulados por SistemaArchivos. @author Luciano */
final class EntradaArchivo {
    private final long id;
    private final TipoEntrada tipo;
    private final String dueno;
    private final PermisoArchivo permiso;
    private String nombre;

    EntradaArchivo(long id, String nombre, TipoEntrada tipo, String dueno, PermisoArchivo permiso) {
        if (id < 0 || tipo == null || dueno == null || dueno.isBlank() || permiso == null)
            throw new IllegalArgumentException("Entrada de archivo invalida");
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.dueno = dueno;
        this.permiso = permiso;
    }

    long getId() { return id; }
    String getNombre() { return nombre; }
    TipoEntrada getTipo() { return tipo; }
    String getDueno() { return dueno; }
    PermisoArchivo getPermiso() { return permiso; }
    boolean esDirectorio() { return tipo == TipoEntrada.DIRECTORIO; }
    void renombrar(String nuevoNombre) { nombre = nuevoNombre; }
}
