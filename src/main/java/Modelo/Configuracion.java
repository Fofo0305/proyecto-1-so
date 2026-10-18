package Modelo;

/**
 * Parámetros iniciales del clúster, independientes de las ventanas.
 * @author Adolfo
 */
public final class Configuracion {
    private final int[] ram;
    private final int duracionMs;
    private final int quantum;
    private final int latencia;

    public Configuracion(int[] ram, int duracionMs, int quantum, int latencia) {
        if (ram == null || ram.length < 2)
            throw new IllegalArgumentException("Se requieren al menos dos computadores");
        for (int capacidad : ram)
            if (capacidad < 1) throw new IllegalArgumentException("La RAM de cada computador debe ser positiva");
        if (duracionMs < 1 || duracionMs > 60000)
            throw new IllegalArgumentException("El ciclo debe durar entre 1 y 60000 ms");
        if (quantum < 1) throw new IllegalArgumentException("El quantum debe ser positivo");
        if (latencia < 0) throw new IllegalArgumentException("La latencia no puede ser negativa");
        this.ram = ram.clone();
        this.duracionMs = duracionMs;
        this.quantum = quantum;
        this.latencia = latencia;
    }

    public int getCantidadComputadores() { return ram.length; }
    public int[] getRam() { return ram.clone(); }
    public int getDuracionMs() { return duracionMs; }
    public int getQuantum() { return quantum; }
    public int getLatencia() { return latencia; }
    public PoliticaPlanificacion getPoliticaInicial() { return PoliticaPlanificacion.FCFS; }
}
