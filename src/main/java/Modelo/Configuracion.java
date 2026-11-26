package Modelo;

/**
 * Parámetros iniciales de la simulación, independientes de las ventanas.
 * El disco empieza con {@link #BLOQUES_DISCO_POR_DEFECTO} bloques, {@link #SWAP_POR_DEFECTO}
 * de swap, FIFO y el cabezal en 0; {@link #conDisco} devuelve una copia con otros valores.
 * @author Adolfo
 */
public final class Configuracion {
    public static final int BLOQUES_DISCO_POR_DEFECTO = 200;
    public static final int SWAP_POR_DEFECTO = 40;
    private final int[] ram;
    private final int duracionMs;
    private final int quantum;
    private PoliticaPlanificacion[] politicas;
    private int[] quantums;
    private int bloquesDisco = BLOQUES_DISCO_POR_DEFECTO;
    private int bloquesSwap = SWAP_POR_DEFECTO;
    private PoliticaDisco politicaDisco = PoliticaDisco.FIFO;
    private int cabezalInicial;

    public Configuracion(int[] ram, int duracionMs, int quantum) {
        if (ram == null || ram.length != 1)
            throw new IllegalArgumentException("Proyecto 2 requiere exactamente un computador");
        for (int capacidad : ram)
            if (capacidad < 1) throw new IllegalArgumentException("La RAM de cada computador debe ser positiva");
        if (duracionMs < 1 || duracionMs > 60000)
            throw new IllegalArgumentException("El ciclo debe durar entre 1 y 60000 ms");
        if (quantum < 1) throw new IllegalArgumentException("El quantum debe ser positivo");
        this.ram = ram.clone();
        this.duracionMs = duracionMs;
        this.quantum = quantum;
        politicas=new PoliticaPlanificacion[ram.length];quantums=new int[ram.length];
        for(int i=0;i<ram.length;i++){politicas[i]=PoliticaPlanificacion.FCFS;quantums[i]=quantum;}
    }

    public Configuracion(int[] ram, int ms, int q, PoliticaPlanificacion[] p, int[] qs) {
        this(ram, ms, q);
        if(p==null || qs==null || p.length!=ram.length || qs.length!=ram.length) throw new IllegalArgumentException("Politicas incompletas");
        for(int i=0;i<p.length;i++) if(p[i]==null || qs[i]<1) throw new IllegalArgumentException("Politica o quantum invalido");
        politicas=p.clone();quantums=qs.clone();
    }
    public PoliticaPlanificacion[] getPoliticas(){return politicas.clone();}
    public int[] getQuantums(){return quantums.clone();}
    public int getCantidadComputadores() { return ram.length; }
    public int[] getRam() { return ram.clone(); }
    public int getDuracionMs() { return duracionMs; }
    public int getQuantum() { return quantum; }
    public PoliticaPlanificacion getPoliticaInicial() { return PoliticaPlanificacion.FCFS; }

    /**
     * Copia con otro disco. Los limites son los mismos que exige el disco, para avisar
     * desde el formulario antes de construir la simulacion.
     */
    public Configuracion conDisco(int bloques, int swap, PoliticaDisco politica, int cabezal) {
        if (bloques < 100 || bloques > 500)
            throw new IllegalArgumentException("El disco debe tener entre 100 y 500 bloques");
        if (swap < 1 || swap >= bloques)
            throw new IllegalArgumentException("El area swap debe tener entre 1 y " + (bloques - 1) + " bloques");
        if (cabezal < 0 || cabezal >= bloques)
            throw new IllegalArgumentException("El cabezal debe estar entre 0 y " + (bloques - 1));
        if (politica == null) throw new IllegalArgumentException("La politica de disco es obligatoria");
        Configuracion copia = new Configuracion(ram, duracionMs, quantum, politicas, quantums);
        copia.bloquesDisco = bloques;
        copia.bloquesSwap = swap;
        copia.politicaDisco = politica;
        copia.cabezalInicial = cabezal;
        return copia;
    }

    /** Copia con los datos de disco de {@code otra}; sirve al cambiar politica o ciclo. */
    public Configuracion conDiscoDe(Configuracion otra) {
        return conDisco(otra.bloquesDisco, otra.bloquesSwap, otra.politicaDisco, otra.cabezalInicial);
    }

    public int getBloquesDisco() { return bloquesDisco; }
    public int getBloquesSwap() { return bloquesSwap; }
    public PoliticaDisco getPoliticaDisco() { return politicaDisco; }
    public int getCabezalInicial() { return cabezalInicial; }
}
