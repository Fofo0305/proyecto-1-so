package Pruebas;
import Interfaces.PanelPlanificacion;
import Sistema.ControladorSimulacion;
import Modelo.*;
import javax.swing.*;
import java.awt.*;
import static Pruebas.PruebaSemaforos.verificar;
/** Acciones Swing reales, solicitud invalida y callbacks asincronos. */
public final class PruebaPanelPlanificacion {
 public static void main(String[] args)throws Exception {
  try(ControladorSimulacion c=new ControladorSimulacion()) {
   c.configurar(new Configuracion(new int[]{32,32},10,4,2));
   PanelPlanificacion[] panel=new PanelPlanificacion[1];
   SwingUtilities.invokeAndWait(()->{panel[0]=new PanelPlanificacion(c);boton(panel[0],"Crear proceso de E/S").doClick();});
   esperar(()->c.getCargaInicial().getProcesos().length==1);
   verificar(c.getCargaInicial().getProcesos()[0].parametrosES().equals(new ParametrosES(2,3)),"Formulario pierde E/S");
   SwingUtilities.invokeAndWait(()->{
    try {
     var campo=PanelPlanificacion.class.getDeclaredField("politica");campo.setAccessible(true);
     ((JComboBox<?>)campo.get(panel[0])).setSelectedItem(PoliticaPlanificacion.ROUND_ROBIN);
     boton(panel[0],"Aplicar politica y quantum").doClick();
    }catch(Exception e){throw new RuntimeException(e);}
   });
   esperar(()->c.getComputadores()[0].getPolitica()==PoliticaPlanificacion.ROUND_ROBIN);
   verificar(c.getComputadores()[1].getPolitica()==PoliticaPlanificacion.FCFS,"GUI cambio otro computador");
   SwingUtilities.invokeAndWait(()->{
    try{var f=PanelPlanificacion.class.getDeclaredField("es");f.setAccessible(true);((JTextField[])f.get(panel[0]))[6].setText("0");boton(panel[0],"Crear proceso de E/S").doClick();}
    catch(Exception e){throw new RuntimeException(e);}
   });
   esperar(()->{
    boolean[] listo={false};SwingUtilities.invokeAndWait(()->{try{var f=PanelPlanificacion.class.getDeclaredField("resultado");f.setAccessible(true);listo[0]=((JLabel)f.get(panel[0])).getText().contains("positivas");}catch(Exception e){throw new RuntimeException(e);}});return listo[0];
   });
   verificar(c.getCargaInicial().getProcesos().length==1,"GUI invalida creo proceso");
  }
  System.out.println("OK: botones de politica y E/S, validaciones y operacion asincrona Swing");
 }
 static JButton boton(Container p,String texto){for(Component x:p.getComponents()){if(x instanceof JButton b && b.getText().equals(texto))return b;if(x instanceof Container c){JButton b=boton(c,texto);if(b!=null)return b;}}return null;}
 static void esperar(java.util.concurrent.Callable<Boolean> condicion)throws Exception{long limite=System.nanoTime()+3_000_000_000L;while(System.nanoTime()<limite){if(condicion.call())return;Thread.sleep(10);}throw new AssertionError("Solicitud Swing no termino");}
}
