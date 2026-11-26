package Modelo;

/**
 * Resultado de validar una operacion CRUD antes de mandarla al disco.
 * @param bloqueObjetivo bloque donde el cabezal debe ir para atenderla
 * @param bloquesTrabajo bloques que el disco lee o escribe; cada uno cuesta un ciclo
 * @author Luciano
 */
public record PlanOperacionArchivo(int bloqueObjetivo, int bloquesTrabajo) {
    public PlanOperacionArchivo {
        if (bloqueObjetivo < 0 || bloquesTrabajo < 1) throw new IllegalArgumentException("Plan de disco invalido");
    }
}
