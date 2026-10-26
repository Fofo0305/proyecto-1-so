package Planificacion;

import EDD.Cola;
import Modelo.Proceso;

/**
 * Decide qué proceso listo recibe la CPU. El núcleo protege la selección completa.
 * @author Adolfo
 */
public interface Politica {
    Proceso seleccionar(Cola<Proceso> listos);
    default void ordenar(Cola<Proceso> listos) { }
    default boolean debeApropiar(Proceso actual, Cola<Proceso> listos, int usados, int quantum) { return false; }
}
