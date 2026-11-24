package Modelo;

/**
 * Copia inmutable de un directorio o archivo para el JTree: con sus hijos ya copiados,
 * la interfaz puede recorrerla sin tomar el semaforo del sistema de archivos.
 * En un directorio, tamanoBloques es 0 y primerBloque es -1.
 * @author Luciano
 */
public record VistaEntradaArchivo(String ruta, String nombre, TipoEntrada tipo, int tamanoBloques,
        String dueno, PermisoArchivo permiso, int primerBloque, VistaEntradaArchivo[] hijos) {
    public VistaEntradaArchivo {
        if (ruta == null || !ruta.startsWith("/") || nombre == null || tipo == null || dueno == null
                || dueno.isBlank() || permiso == null || hijos == null
                || tipo == TipoEntrada.ARCHIVO && (tamanoBloques < 1 || primerBloque < 0 || hijos.length > 0)
                || tipo == TipoEntrada.DIRECTORIO && (tamanoBloques != 0 || primerBloque != -1))
            throw new IllegalArgumentException("Vista de entrada invalida");
        hijos = hijos.clone();
        for (VistaEntradaArchivo hijo : hijos)
            if (hijo == null) throw new IllegalArgumentException("Vista de entrada invalida");
    }

    /** Copia de los hijos: modificarla no cambia esta vista. */
    @Override
    public VistaEntradaArchivo[] hijos() { return hijos.clone(); }

    public boolean esDirectorio() { return tipo == TipoEntrada.DIRECTORIO; }

    /** Texto del nodo en el JTree. */
    @Override
    public String toString() { return nombre.isEmpty() ? "/" : nombre; }
}
