package Planificacion;

import EDD.Cola;
import Modelo.Proceso;

/**
 * Turno circular: entrega la CPU al primero de listos y la retira cuando el proceso en
 * ejecucion agota su quantum y hay alguien esperando.
 * @author Antony
 */
public final class RoundRobin implements Politica {

    @Override
    public Proceso seleccionar(Cola<Proceso> listos) {
        return listos.quitar();
    }

    @Override
    public boolean debeApropiar(Proceso actual, Cola<Proceso> listos, int usados, int quantum) {
        return usados >= quantum && listos.getTamano() > 0;
    }
}
