package Interfaces;
import Modelo.VistaMetricas;
import javax.swing.*;
import java.awt.*;
/** Series de utilizacion acumulada; anillo de ultimos 600 ciclos por CPU. */
public final class GraficaUso extends JPanel {
 private VistaMetricas[] vistas=new VistaMetricas[0];
 public GraficaUso(){setPreferredSize(new Dimension(800,250));setBackground(Color.WHITE);}
 public void mostrar(VistaMetricas[] v){vistas=v.clone();repaint();}
 protected void paintComponent(Graphics grafico){super.paintComponent(grafico);Graphics2D g=(Graphics2D)grafico.create();try{
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
  int x=55,y=35,w=Math.max(1,getWidth()-85),h=Math.max(1,getHeight()-80);long minimo=Long.MAX_VALUE,maximo=1;
  for(VistaMetricas v:vistas)for(long c:v.historiaCiclos()){minimo=Math.min(minimo,c);maximo=Math.max(maximo,c);}
  if(minimo==Long.MAX_VALUE)minimo=0;
  g.setColor(Color.DARK_GRAY);g.drawString("Utilizacion acumulada por CPU (%) - ultimos 600 ciclos",x,18);
  for(int i=0;i<=4;i++){int yy=y+h-i*h/4;g.setColor(new Color(225,230,238));g.drawLine(x,yy,x+w,yy);g.setColor(Color.DARK_GRAY);g.drawString(""+(i*25),10,yy+4);}
  g.drawString("Ciclo "+minimo,x,y+h+20);g.drawString(""+maximo,x+w-40,y+h+20);
  for(int i=0;i<vistas.length;i++){VistaMetricas v=vistas[i];g.setColor(Color.getHSBColor((float)i/Math.max(1,vistas.length),.8f,.65f));g.setStroke(new BasicStroke(2));g.drawString("C"+v.computador(),x+400+(i%6)*48,18+(i/6)*14);long[] cs=v.historiaCiclos();double[] us=v.historiaUso();for(int j=1;j<cs.length;j++){int a=x+(int)((cs[j-1]-minimo)*w/Math.max(1,maximo-minimo)),b=x+(int)((cs[j]-minimo)*w/Math.max(1,maximo-minimo));g.drawLine(a,y+h-(int)(us[j-1]*h/100),b,y+h-(int)(us[j]*h/100));}}
 }finally{g.dispose();}}
}
