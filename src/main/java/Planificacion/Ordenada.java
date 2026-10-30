package Planificacion;

import EDD.Cola;
import Modelo.Proceso;

/**
 * Base de las politicas que prefieren el valor numerico menor. La ordenacion es una
 * insercion estable propia: los empates conservan el orden de llegada a listos.
 * @author Antony
 */
public abstract class Ordenada implements Politica {

    /** Valor que ordena al proceso dentro de la cola; gana el menor. */
    protected abstract int clave(Proceso proceso);

    @Override
    public void ordenar(Cola<Proceso> listos) {
        Object[] procesos = listos.instantanea();
        for (int actual = 1; actual < procesos.length; actual++) {
            Object movido = procesos[actual];
            int anterior = actual - 1;
            while (anterior >= 0 && clave((Proceso) procesos[anterior]) > clave((Proceso) movido)) {
                procesos[anterior + 1] = procesos[anterior];
                anterior--;
            }
            procesos[anterior + 1] = movido;
        }
        for (int posicion = 0; posicion < procesos.length; posicion++) {
            listos.quitar();
        }
        for (Object proceso : procesos) {
            listos.agregar((Proceso) proceso);
        }
    }

    @Override
    public Proceso seleccionar(Cola<Proceso> listos) {
        ordenar(listos);
        return listos.quitar();
    }

    @Override
    public boolean debeApropiar(Proceso actual, Cola<Proceso> listos, int usados, int quantum) {
        ordenar(listos);
        return listos.getTamano() > 0 && clave(listos.primero()) < clave(actual);
    }
}
