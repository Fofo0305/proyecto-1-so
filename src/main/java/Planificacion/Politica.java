package Planificacion;

import EDD.Cola;
import Modelo.Proceso;

/**
 * Decide qué proceso listo recibe la CPU. El núcleo protege la selección completa.
 * @author Adolfo
 */
public interface Politica {
    Proceso seleccionar(Cola<Proceso> listos);
}
