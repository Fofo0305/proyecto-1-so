package Sistema;

import Modelo.AreaDisco;
import Modelo.DireccionDisco;
import Modelo.VistaBloqueDisco;
import java.util.concurrent.Semaphore;

/** Mapa del unico disco local de AvilaOS. @author Adolfo */
public final class Disco {
    public static final int MINIMO_BLOQUES = 100;
    public static final int MAXIMO_BLOQUES = 500;

    private final Semaphore mutex = new Semaphore(1, true);
    private final BloqueDisco[] bloques;
    private final int tamanoSwap;
    private int posicionCabezal;
    private DireccionDisco direccion = DireccionDisco.ASCENDENTE;

    public Disco(int totalBloques, int bloquesSwap, int cabezalInicial) {
        if (totalBloques < MINIMO_BLOQUES || totalBloques > MAXIMO_BLOQUES)
            throw new IllegalArgumentException("El disco debe tener entre 100 y 500 bloques");
        if (bloquesSwap < 1 || bloquesSwap >= totalBloques)
            throw new IllegalArgumentException("El area swap debe ocupar una parte no vacia del disco");
        if (cabezalInicial < 0 || cabezalInicial >= totalBloques)
            throw new IllegalArgumentException("El cabezal inicial esta fuera del disco");
        tamanoSwap = bloquesSwap;
        posicionCabezal = cabezalInicial;
        bloques = new BloqueDisco[totalBloques];
        int inicioSwap = totalBloques - bloquesSwap;
        for (int numero = 0; numero < bloques.length; numero++)
            bloques[numero] = new BloqueDisco(numero, numero < inicioSwap ? AreaDisco.DATOS : AreaDisco.SWAP);
    }

    public VistaBloqueDisco[] consultarBloques() {
        mutex.acquireUninterruptibly();
        try {
            VistaBloqueDisco[] copia = new VistaBloqueDisco[bloques.length];
            for (int i = 0; i < copia.length; i++) copia[i] = bloques[i].consultar();
            return copia;
        } finally { mutex.release(); }
    }

    public int getTotalBloques() { return bloques.length; }
    public int getTamanoSwap() { return tamanoSwap; }

    public int getPosicionCabezal() {
        mutex.acquireUninterruptibly();
        try { return posicionCabezal; }
        finally { mutex.release(); }
    }

    public DireccionDisco getDireccion() {
        mutex.acquireUninterruptibly();
        try { return direccion; }
        finally { mutex.release(); }
    }
}
