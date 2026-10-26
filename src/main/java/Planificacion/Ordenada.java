package Planificacion;
import EDD.Cola;
import Modelo.Proceso;
/** Insercion estable propia; los empates conservan el orden de llegada a listos. */
public abstract class Ordenada implements Politica {
 protected abstract int clave(Proceso p);
 public void ordenar(Cola<Proceso> listos) {
  Object[] a=listos.instantanea();
  for(int i=1;i<a.length;i++){ Object x=a[i]; int j=i-1; while(j>=0 && clave((Proceso)a[j])>clave((Proceso)x)){a[j+1]=a[j];j--;} a[j+1]=x; }
  for(int i=0;i<a.length;i++) listos.quitar();
  for(Object p:a) listos.agregar((Proceso)p);
 }
 public Proceso seleccionar(Cola<Proceso> listos) { ordenar(listos); return listos.quitar(); }
 public boolean debeApropiar(Proceso actual,Cola<Proceso> listos,int usados,int quantum) { ordenar(listos); return listos.getTamano()>0 && clave(listos.primero())<clave(actual); }
}
