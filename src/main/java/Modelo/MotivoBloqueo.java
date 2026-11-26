package Modelo;

/**
 * Motivos por los que un proceso puede quedar bloqueado.
 * @author Adolfo
 */
public enum MotivoBloqueo {
    NINGUNO,
    /** Espera temporal configurada con rafaga y duracion. */
    ENTRADA_SALIDA,
    /** Espera a que el disco termine la solicitud CRUD del proceso. */
    DISCO
}
