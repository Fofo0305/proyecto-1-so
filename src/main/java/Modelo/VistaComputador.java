package Modelo;

/**
 * Lectura inmutable de un computador para la interfaz gráfica.
 * @author Adolfo
 */
public record VistaComputador(int id, long ciclo, ModoCPU modo, int usada, int libre,
        String cpu, String nuevos, String listos, String bloqueados, String terminados) {
}
