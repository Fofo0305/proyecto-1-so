package Planificacion;
import EDD.Cola;
import Modelo.Proceso;
public final class RoundRobin implements Politica {
 public Proceso seleccionar(Cola<Proceso> listos) { return listos.quitar(); }
 public boolean debeApropiar(Proceso actual,Cola<Proceso> listos,int usados,int quantum) { return usados>=quantum && listos.getTamano()>0; }
}
