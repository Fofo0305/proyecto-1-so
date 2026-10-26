package Planificacion;
import Modelo.Proceso;
public final class EDF extends Ordenada { protected int clave(Proceso p) { return p.getDeadline(); } }
