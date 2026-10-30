package Modelo;

/** Totales de espera, incluyendo episodios abiertos y cancelados. @author Luciano Rojas */
public record VistaBloqueosBuffer(String nombre, long ciclosSemaforo, long esperasSemaforo,
        long abiertasSemaforo, long ciclosRed, long esperasRed, long abiertasRed) {
    public double promedioSemaforo() { return esperasSemaforo == 0 ? 0 : (double) ciclosSemaforo / esperasSemaforo; }
    public double promedioRed() { return esperasRed == 0 ? 0 : (double) ciclosRed / esperasRed; }
}
