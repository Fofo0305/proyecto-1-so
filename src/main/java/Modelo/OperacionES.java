package Modelo;

/** Operaciones que un proceso puede solicitar al disco. @author Adolfo */
public enum OperacionES {
    CREAR,
    LEER,
    ACTUALIZAR,
    ELIMINAR,
    SUSPENDER,
    REACTIVAR;

    /** Solo las operaciones del sistema de archivos apuntan a una ruta. */
    public boolean requiereRuta() {
        return this == CREAR || this == LEER || this == ACTUALIZAR || this == ELIMINAR;
    }
}
