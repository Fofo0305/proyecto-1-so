package Pruebas;
import Modelo.*;
import Sistema.*;
import Persistencia.ArchivoConfiguracion;
import java.nio.file.*;
import static Pruebas.PruebaSemaforos.verificar;
public final class PruebaIntegrante3 {
 static ControladorSimulacion crear(){ControladorSimulacion c=new ControladorSimulacion();c.configurar(new Configuracion(new int[]{64,64},1,2,3));return c;}
 public static void main(String[] args)throws Exception{
  for(PoliticaPlanificacion politica:PoliticaPlanificacion.values()) probar(politica);
  try(ControladorSimulacion c=crear()){
   c.cambiarPolitica(1,PoliticaPlanificacion.ROUND_ROBIN,2);
   Proceso a=c.crearProceso("A",1,3,4,8,100), b=c.crearProceso("B",1,1,4,8,100);
   c.avanzarCiclo();c.avanzarCiclo();c.avanzarCiclo();
   verificar(a.getPc()==2 && b.getPc()==1,"RR no rota al vencer quantum");
   c.cambiarPolitica(1,PoliticaPlanificacion.PRIORIDADES,1);c.avanzarCiclo();
   verificar(b.getPc()==2 && c.getComputadores()[1].getPolitica()==PoliticaPlanificacion.FCFS,"Cambio afecta otro computador");
   c.cambiarDuracion(20);c.cambiarLatencia(1);
   verificar(c.getCargaInicial().getConfiguracion().getPoliticas()[0]==PoliticaPlanificacion.PRIORIDADES,"Ajustes borran politica persistida");
  }
  for(PoliticaPlanificacion politica:new PoliticaPlanificacion[]{PoliticaPlanificacion.EDF,PoliticaPlanificacion.PRIORIDADES})try(ControladorSimulacion c=crear()){
   c.cambiarPolitica(1,politica,2);Proceso lento=c.crearProceso("Lento",1,8,4,20,100);c.avanzarCiclo();
   Proceso urgente=c.crearProceso("Urgente",1,0,4,2,8);c.avanzarCiclo();
   verificar(lento.getPc()==1 && urgente.getPc()==1,"No apropia ante llegada urgente");
  }
  try(ControladorSimulacion c=crear()){
   Proceso io=c.crearProcesoES("ES",1,1,4,6,100,2,3);c.avanzarCiclo();c.avanzarCiclo();
   verificar(io.getMotivoBloqueo()==MotivoBloqueo.ENTRADA_SALIDA && io.getPc()==2,"No bloquea al agotar rafaga");
   c.avanzarCiclo();c.avanzarCiclo();verificar(io.getPc()==2 && io.getEstado()==EstadoProceso.BLOQUEADO,"E/S demasiado corta");
   c.avanzarCiclo();verificar(io.getEstado()==EstadoProceso.LISTO,"No retorna al cumplir E/S");c.avanzarCiclo();verificar(io.getPc()==3,"No conserva PC tras E/S");
   Proceso vencido=c.crearProcesoES("Vence",2,1,4,20,3,1,10);for(int i=0;i<4;i++)c.avanzarCiclo();
   verificar(vencido.getResultado().equals("VENCIDO") && c.getComputadores()[1].getMemoria().getUsada()==0,"E/S vencida conserva RAM");
   Path archivo=Path.of("target/pruebas-buffers/io-roundtrip.csv");c.cambiarPolitica(1,PoliticaPlanificacion.EDF,5);ArchivoConfiguracion.guardar(archivo,c.getCargaInicial());
   c.cargarCarga(ArchivoConfiguracion.cargar(archivo));verificar(c.getComputadores()[0].getPolitica()==PoliticaPlanificacion.EDF && c.getComputadores()[0].getQuantum()==5,"Pierde politica CSV");
   verificar(c.getCargaInicial().getProcesos()[0].parametrosES().equals(new ParametrosES(2,3)),"Pierde parametros E/S");
  }
  System.out.println("OK: cuatro politicas, apropiacion, E/S exacta, metricas, CSV3 y buffers bajo RR");
 }
 static void probar(PoliticaPlanificacion politica)throws Exception{
  try(ControladorSimulacion c=crear()){
   c.cambiarPolitica(1,politica,1);c.cambiarPolitica(2,politica,2);c.crearBuffer("Datos",1,1);
   c.crearProcesoBuffer("P",2,0,4,300,TipoProceso.PRODUCTOR,"Datos",1,4);
   c.crearProcesoBuffer("C",1,1,4,300,TipoProceso.CONSUMIDOR,"Datos",1,4);
   c.crearProceso("CPU",1,2,4,7,300);c.crearProcesoES("ES",2,2,4,6,300,2,3);
   for(int i=0;i<160;i++)c.avanzarCiclo();
   VistaContadoresBuffer b=c.consultarContadoresBuffers()[0];verificar(b.producidos()==4 && b.consumidos()==4,"Politica rompe operaciones buffer "+politica);
   for(VistaMetricas m:c.consultarMetricas())verificar(m.completados()==2 && m.vencidos()==0 && m.ocupados()<=m.ciclos() && m.cumplimiento()==100 && m.equidad()>0 && m.equidad()<=1,"Metricas incorrectas "+politica);
   verificar(c.consultarLog().length>0,"Falta log global");
  }
 }
}
