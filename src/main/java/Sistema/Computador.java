package Sistema;

import Modelo.Memoria;
import Modelo.Proceso;
import EDD.Cola;

/**
 * Computador reutilizable con identificación y RAM propias.
 * @author Adolfo
 */
public class Computador {
    private final int id;
    private final Memoria memoria;
    private final Cola<Proceso> nuevos = new Cola<>();
    private final Cola<Proceso> listos = new Cola<>();
    private final Cola<Proceso> bloqueados = new Cola<>();
    private final Cola<Proceso> terminados = new Cola<>();

    public Computador(int id, int capacidadRam) {
        if (id < 1) {
            throw new IllegalArgumentException("El identificador del computador debe ser positivo");
        }
        this.id = id;
        this.memoria = new Memoria(capacidadRam);
    }

    public int getId() { return id; }
    public Memoria getMemoria() { return memoria; }

    /** Consulta los procesos nuevos sin exponer la cola interna. */
    public Proceso[] getNuevos() { return copiarProcesos(nuevos); }

    /** Consulta los procesos listos sin exponer la cola interna. */
    public Proceso[] getListos() { return copiarProcesos(listos); }

    /** Consulta los procesos bloqueados sin exponer la cola interna. */
    public Proceso[] getBloqueados() { return copiarProcesos(bloqueados); }

    /** Consulta los procesos terminados sin exponer la cola interna. */
    public Proceso[] getTerminados() { return copiarProcesos(terminados); }

    private Proceso[] copiarProcesos(Cola<Proceso> cola) {
        Object[] elementos = cola.instantanea();
        Proceso[] copia = new Proceso[elementos.length];
        for (int i = 0; i < elementos.length; i++) {
            copia[i] = (Proceso) elementos[i];
        }
        return copia;
    }
}
