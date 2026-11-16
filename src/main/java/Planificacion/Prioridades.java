package Planificacion;

import Modelo.Proceso;

/**
 * Prioridad estatica: un numero menor representa mayor prioridad.
 * @author Antony
 */
public final class Prioridades extends Ordenada {

    @Override
    protected int clave(Proceso proceso) {
        return proceso.getPrioridad();
    }
}
