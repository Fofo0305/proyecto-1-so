package Pruebas;
import Modelo.*;import Sistema.*;import java.nio.file.*;
public final class ExperimentosInforme {
 public static void main(String[] args)throws Exception{
  StringBuilder s=new StringBuilder("escenario,politica,ciclos,ocupados,completados,vencidos,respuesta,throughput,utilizacion,cumplimiento,equidad\n");
  for(PoliticaPlanificacion politica:PoliticaPlanificacion.values())try(ControladorSimulacion c=new ControladorSimulacion()){
   c.configurar(new Configuracion(new int[]{64,64},1,2,3));c.cambiarPolitica(1,politica,2);
   c.crearProceso("Largo",1,5,4,18,30);c.crearProceso("Urgente",1,0,4,2,6);c.crearProceso("Medio",1,2,4,4,10);
   for(int i=0;i<30;i++)c.avanzarCiclo();agregar(s,"CPU con plazos",politica,c.consultarMetricas()[0]);
  }
  for(int equipo:new int[]{1,2})try(ControladorSimulacion c=new ControladorSimulacion()){
   c.configurar(new Configuracion(new int[]{64,64},1,2,3));c.crearBuffer("B",1,1);
   c.crearProcesoBuffer("P",equipo,0,4,200,TipoProceso.PRODUCTOR,"B",1,3);c.crearProcesoBuffer("C",1,0,4,200,TipoProceso.CONSUMIDOR,"B",1,3);
   for(int i=0;i<80;i++)c.avanzarCiclo();agregar(s,equipo==1?"Buffer local":"Buffer remoto",PoliticaPlanificacion.FCFS,c.consultarMetricas()[0]);
  }
  Files.createDirectories(Path.of("entrega"));
  Files.writeString(Path.of("entrega/experimentos.csv"),s);System.out.print(s);
 }
 static void agregar(StringBuilder s,String nombre,PoliticaPlanificacion p,VistaMetricas m){s.append(String.format(java.util.Locale.ROOT,"%s,%s,%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f%n",nombre,p,m.ciclos(),m.ocupados(),m.completados(),m.vencidos(),m.respuesta(),m.throughput(),m.utilizacion(),m.cumplimiento(),m.equidad()));}
}
