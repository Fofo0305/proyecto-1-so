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
    TERMINADO;

    /** Define en un solo lugar el automata permitido del PCB. */
    public boolean permiteTransicionA(EstadoProceso destino) {
        if (destino == null) return false;
        return switch (this) {
            case NUEVO -> destino == LISTO || destino == TERMINADO;
            case LISTO -> destino == EJECUCION || destino == LISTO_SUSPENDIDO || destino == TERMINADO;
            case EJECUCION -> destino == LISTO || destino == BLOQUEADO || destino == TERMINADO;
            case BLOQUEADO -> destino == LISTO || destino == BLOQUEADO_SUSPENDIDO || destino == TERMINADO;
            case LISTO_SUSPENDIDO -> destino == LISTO || destino == TERMINADO;
            case BLOQUEADO_SUSPENDIDO -> destino == BLOQUEADO || destino == LISTO_SUSPENDIDO
                    || destino == TERMINADO;
            case TERMINADO -> false;
        };
    }
}
