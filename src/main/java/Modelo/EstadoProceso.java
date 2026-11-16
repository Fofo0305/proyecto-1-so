package Modelo;

/**
 * Estados de un proceso durante la simulación.
 * @author Adolfo
 */
public enum EstadoProceso {
    NUEVO,
    LISTO,
    EJECUCION,
    BLOQUEADO,
    LISTO_SUSPENDIDO,
    BLOQUEADO_SUSPENDIDO,
    TERMINADO
}
