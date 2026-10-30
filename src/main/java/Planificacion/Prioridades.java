package Planificacion;
import Modelo.Proceso;
/** Menor numero representa mayor prioridad. */
public final class Prioridades extends Ordenada { protected int clave(Proceso p) { return p.getPrioridad(); } }
