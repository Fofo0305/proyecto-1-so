package Planificacion;

import Modelo.Proceso;

/**
 * Earliest Deadline First: gana el proceso cuyo plazo vence antes.
 * @author Antony
 */
public final class EDF extends Ordenada {

    @Override
    protected int clave(Proceso proceso) {
        return proceso.getDeadline();
    }
}
