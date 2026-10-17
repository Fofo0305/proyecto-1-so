package Sistema;

import Modelo.Memoria;

/**
 * Computador reutilizable con identificación y RAM propias.
 * @author Adolfo
 */
public class Computador {
    private final int id;
    private final Memoria memoria;

    public Computador(int id, int capacidadRam) {
        if (id < 1) {
            throw new IllegalArgumentException("El identificador del computador debe ser positivo");
        }
        this.id = id;
        this.memoria = new Memoria(capacidadRam);
    }

    public int getId() { return id; }
    public Memoria getMemoria() { return memoria; }
}
