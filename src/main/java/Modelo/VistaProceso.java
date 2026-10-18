package Modelo;

/** Datos de un PCB capturados por el computador para dibujar su tarjeta. @author Adolfo */
public record VistaProceso(long id, String nombre, int memoria, int prioridad,
        int deadline, int restante, int pc, int mar, EstadoProceso estado) { }
