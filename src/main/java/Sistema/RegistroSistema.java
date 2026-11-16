package Sistema;

import Modelo.EventoSistema;
import java.util.concurrent.Semaphore;

/**
 * Anillo acotado de eventos de un computador; no adquiere bloqueos de otros nucleos.
 * Al llenarse descarta el evento mas antiguo, pero la secuencia local sigue creciendo.
 * @author Antony
 */
public final class RegistroSistema {

    private static final int CAPACIDAD = 1024;

    private final EventoSistema[] datos = new EventoSistema[CAPACIDAD];
    private final Semaphore mutex = new Semaphore(1);
    private int inicio;
    private int tamano;
    private long secuencia;

    /** Anota un evento; si el anillo esta lleno reemplaza al mas antiguo. */
    public void agregar(long ciclo, int computador, long proceso, String tipo, String detalle) {
        mutex.acquireUninterruptibly();
        try {
            EventoSistema evento = new EventoSistema(++secuencia, ciclo, computador, proceso, tipo, detalle);
            if (tamano < datos.length) {
                datos[(inicio + tamano++) % datos.length] = evento;
            } else {
                datos[inicio] = evento;
                inicio = (inicio + 1) % datos.length;
            }
        } finally {
            mutex.release();
        }
    }

    /** Copia los eventos retenidos, del mas antiguo al mas reciente. */
    public EventoSistema[] consultar() {
        mutex.acquireUninterruptibly();
        try {
            EventoSistema[] copia = new EventoSistema[tamano];
            for (int posicion = 0; posicion < tamano; posicion++) {
                copia[posicion] = datos[(inicio + posicion) % datos.length];
            }
            return copia;
        } finally {
            mutex.release();
        }
    }
}
