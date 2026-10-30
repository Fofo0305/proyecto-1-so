package Planificacion;

import EDD.Cola;
import Modelo.Proceso;

/**
 * Selecciona el primer proceso que llegó a listos sin ejecutar instrucciones.
 * @author Adolfo
 */
public class FCFS implements Politica {
    @Override
    public Proceso seleccionar(Cola<Proceso> listos) {
        if (listos == null) {
            throw new IllegalArgumentException("La cola de listos es obligatoria");
        }
        return listos.quitar();
    }
}
