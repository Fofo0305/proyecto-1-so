package Pruebas;
import Sistema.*;import Modelo.*;
import static Pruebas.PruebaSemaforos.verificar;
/** Reloj real y cambios concurrentes de politica y lecturas, con cierre acotado. */
public final class PruebaRelojIntegrante3 {
 public static void main(String[] args)throws Exception{
  try(ControladorSimulacion c=new ControladorSimulacion()){
   c.configurar(new Configuracion(new int[]{64,64},2,2,2));c.crearBuffer("B",2,1);
   c.crearProcesoBuffer("P",2,1,4,1000,TipoProceso.PRODUCTOR,"B",1,10);c.crearProcesoBuffer("C",1,1,4,1000,TipoProceso.CONSUMIDOR,"B",1,10);
   c.crearProcesoES("IO",1,2,4,30,1000,2,2);c.iniciar();
   for(int i=0;i<80;i++){c.cambiarPolitica(1,PoliticaPlanificacion.values()[i%4],1+i%3);c.cambiarPolitica(2,PoliticaPlanificacion.values()[(i+1)%4],2);c.consultarObservacionBuffers();c.consultarMetricas();c.consultarLog();Thread.sleep(2);}
   c.pausar();long ciclo=c.getComputadores()[0].getUltimoCiclo();Thread.sleep(20);verificar(c.getComputadores()[0].getUltimoCiclo()==ciclo,"Pausa no estable");
   c.reanudar();Thread.sleep(40);c.detener();verificar(c.getComputadores()[0].getUltimoCiclo()==c.getComputadores()[1].getUltimoCiclo(),"Barrera inconsistente");
  }
  System.out.println("OK: reloj real, 160 cambios de politica, consultas, pausa y cierre sin bloqueo");
 }
}
