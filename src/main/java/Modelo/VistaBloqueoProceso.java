package Modelo;

/** Datos propios del proceso, capturados por su nucleo. @author Luciano Rojas */
public record VistaBloqueoProceso(long proceso, String nombre, int computador, String buffer,
        long ciclosSemaforo, long esperasSemaforo, long ciclosRed, long esperasRed, MotivoBloqueo esperaActual) {
    public double promedioSemaforo() { return esperasSemaforo == 0 ? 0 : (double) ciclosSemaforo / esperasSemaforo; }
    public double promedioRed() { return esperasRed == 0 ? 0 : (double) ciclosRed / esperasRed; }
}
