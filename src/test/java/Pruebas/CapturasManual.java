package Pruebas;
import Interfaces.*;
import Modelo.*;
import Persistencia.ArchivoConfiguracion;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
/** Capturas reproducibles del programa real, no maquetas. */
public final class CapturasManual {
 static VentanaPrincipal ventana; static JTabbedPane tabs;
 public static void main(String[] args)throws Exception{
  Files.createDirectories(Path.of("entrega/capturas"));
  SwingUtilities.invokeAndWait(()->{ventana=new VentanaPrincipal();ventana.setSize(1180,900);ventana.setVisible(true);tabs=buscarTabs(ventana.getContentPane());});
  try{
   capturar(0,"01-configuracion");
   var carga=ArchivoConfiguracion.cargar(Path.of("ejemplos/mixto-completo.csv"));ventana.getControlador().cargarCarga(carga);
   SwingUtilities.invokeAndWait(()->{try{var metodo=VentanaPrincipal.class.getDeclaredMethod("mostrarCarga",Modelo.CargaInicial.class);metodo.setAccessible(true);metodo.invoke(ventana,carga);}catch(Exception e){throw new RuntimeException(e);}});
   for(int i=1;i<=3;i++)capturar(i,String.format("%02d-formulario",i+1));
   for(int i=0;i<8;i++)ventana.getControlador().avanzarCiclo();
   SwingUtilities.invokeAndWait(()->{ventana.getPanelComputadores().mostrarDetallesPCB(true);});
   Thread.sleep(500);capturar(4,"05-simulacion-pcb");
   capturar(5,"06-buffers-estado");
   for(int i=0;i<2;i++){final int n=i+1;SwingUtilities.invokeAndWait(()->buscarTabs((Container)tabs.getComponentAt(5)).setSelectedIndex(n));Thread.sleep(100);guardar("0"+(7+i)+"-buffers-"+(i==0?"procesos":"eventos"));}
   capturar(6,"09-politicas-es");
   for(int i=8;i<75;i++)ventana.getControlador().avanzarCiclo();
   Thread.sleep(500);
   SwingUtilities.invokeAndWait(()->buscarTabs((Container)tabs.getComponentAt(6)).setSelectedIndex(1));Thread.sleep(400);guardar("10-metricas-grafica");
   SwingUtilities.invokeAndWait(()->buscarTabs((Container)tabs.getComponentAt(6)).setSelectedIndex(2));Thread.sleep(100);guardar("11-log-global");
   for(int i=1;i<=2;i++){var v=ventana.getControlador().consultarMetricas()[i-1];if(v.completados()!= (i==1?3:1))throw new AssertionError("La carga mixta no completo todos los procesos");}
   System.out.println("OK: once capturas del programa real y carga mixta finalizada");
  }finally{ventana.getControlador().close();SwingUtilities.invokeAndWait(()->ventana.dispose());}
 }
 static JTabbedPane buscarTabs(Container c){for(Component x:c.getComponents()){if(x instanceof JTabbedPane t)return t;if(x instanceof Container hijo){JTabbedPane t=buscarTabs(hijo);if(t!=null)return t;}}return null;}
 static void capturar(int i,String nombre)throws Exception{SwingUtilities.invokeAndWait(()->tabs.setSelectedIndex(i));Thread.sleep(400);guardar(nombre);}
 static void guardar(String nombre)throws Exception{SwingUtilities.invokeAndWait(()->{try{ventana.validate();BufferedImage img=new BufferedImage(ventana.getWidth(),ventana.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();ventana.printAll(g);g.dispose();javax.imageio.ImageIO.write(img,"png",Path.of("entrega/capturas/"+nombre+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}});}
}
