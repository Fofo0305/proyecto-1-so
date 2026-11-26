package Modelo;

/**
 * Datos de una operacion CRUD que no caben en la ruta: quien la pide, el tamano y permiso
 * del archivo a crear y, al actualizar, el nombre nuevo. El proceso los lleva en su PCB
 * para que el sistema de archivos aplique los permisos de quien lo creo.
 * @param tamanoBloques bloques del archivo nuevo (solo CREAR)
 * @param permiso visibilidad del archivo nuevo (solo CREAR)
 * @param nuevoNombre nombre nuevo al ACTUALIZAR; null reescribe el archivo en su sitio
 * @param modo modo de la sesion que creo el proceso
 * @param usuario usuario de esa sesion
 * @author Luciano
 */
public record ParametrosArchivo(int tamanoBloques, PermisoArchivo permiso, String nuevoNombre,
        ModoUsuario modo, String usuario) {
    /** Lo que usa un proceso sin datos extra: lo lanza el administrador, archivo de 1 bloque publico. */
    public static final ParametrosArchivo ADMINISTRADOR =
            new ParametrosArchivo(1, PermisoArchivo.PUBLICO, null, ModoUsuario.ADMINISTRADOR, "admin");

    public ParametrosArchivo {
        if (tamanoBloques < 1) throw new IllegalArgumentException("El archivo necesita al menos un bloque");
        if (permiso == null || modo == null) throw new IllegalArgumentException("Permiso y modo son obligatorios");
        if (usuario == null || usuario.isBlank()) throw new IllegalArgumentException("El usuario es obligatorio");
        if (nuevoNombre != null && nuevoNombre.isBlank()) nuevoNombre = null;
    }

    /** Copia con otra identidad, para lanzar el proceso con la sesion actual. */
    public ParametrosArchivo comoSesion(ModoUsuario otroModo, String otroUsuario) {
        return new ParametrosArchivo(tamanoBloques, permiso, nuevoNombre, otroModo, otroUsuario);
    }
}
