package Modelo;
/** Utilizacion ocupada incluye instrucciones de usuario y operaciones de sistema de procesos.
 * Equidad Jain usa instrucciones recibidas por todos los procesos creados en el equipo. */
public record VistaMetricas(int computador,long ciclos,long ocupados,int creados,int completados,int vencidos,int cancelados,int respondidos,double respuesta,double equidad,double servicio,double servicioCuadrado,long[] historiaCiclos,double[] historiaUso) {
 public VistaMetricas { historiaCiclos=historiaCiclos.clone();historiaUso=historiaUso.clone(); }
 @Override public long[] historiaCiclos(){return historiaCiclos.clone();}
 @Override public double[] historiaUso(){return historiaUso.clone();}
 public double throughput(){return ciclos==0?0:(double)completados/ciclos;}
 public double utilizacion(){return ciclos==0?0:100.0*ocupados/ciclos;}
 public double cumplimiento(){return completados+vencidos==0?0:100.0*completados/(completados+vencidos);}
}
