package Interfaces;
import Modelo.*;
import Sistema.ControladorSimulacion;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Semaphore;

/** Panel de operaciones nuevas; capturas y solicitudes fuera del EDT. */
public final class PanelPlanificacion extends JPanel {
 private final ControladorSimulacion controlador;
 private final JTextArea metricas=texto(),log=texto();
 private final GraficaUso grafica=new GraficaUso();
 private final JLabel resultado=new JLabel("Configure el cluster antes de aplicar cambios.");
 private final JTextField equipo=new JTextField("1",4),quantum=new JTextField("2",4);
 private final JComboBox<PoliticaPlanificacion> politica=new JComboBox<>(PoliticaPlanificacion.values());
 private final JTextField[] es=new JTextField[8];
 private final Semaphore lectura=new Semaphore(1);
 private final Timer timer=new Timer(300,e->actualizar());
 public PanelPlanificacion(ControladorSimulacion controlador) {
  this.controlador=controlador;setLayout(new BorderLayout(8,8));setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
  JTabbedPane tabs=new JTabbedPane();
  JPanel operaciones=new JPanel(new BorderLayout(8,12));
  JPanel plan=new JPanel(new FlowLayout(FlowLayout.LEFT));
  plan.add(new JLabel("Computador"));plan.add(equipo);plan.add(new JLabel("Politica"));plan.add(politica);plan.add(new JLabel("Quantum"));plan.add(quantum);
  JButton aplicar=new JButton("Aplicar politica y quantum");plan.add(aplicar);
  aplicar.addActionListener(e->{String c=equipo.getText(),q=quantum.getText();PoliticaPlanificacion p=(PoliticaPlanificacion)politica.getSelectedItem();trabajar(()->{controlador.cambiarPolitica(Integer.parseInt(c.trim()),p,Integer.parseInt(q.trim()));return "Politica de C"+c+": "+p+" quantum="+q;});});
  operaciones.add(plan,BorderLayout.NORTH);
  JPanel formulario=new JPanel(new GridLayout(0,2,8,8));
  String[] nombres={"Nombre","Computador","Prioridad (0 es la mayor)","RAM","Instrucciones CPU totales","Deadline desde llegada","Rafaga de CPU","Duracion E/S en ciclos"};
  String[] valores={"Tarea E/S","1","1","4","12","150","2","3"};
  for(int i=0;i<es.length;i++){formulario.add(new JLabel(nombres[i]));es[i]=new JTextField(valores[i]);es[i].setName(nombres[i]);formulario.add(es[i]);}
  JPanel esPanel=new JPanel(new BorderLayout(8,8));esPanel.setBorder(BorderFactory.createTitledBorder("Crear proceso IO_BOUND"));esPanel.add(formulario,BorderLayout.CENTER);
  JButton crear=new JButton("Crear proceso de E/S");esPanel.add(crear,BorderLayout.SOUTH);
  crear.addActionListener(e->{String[] v=new String[es.length];for(int i=0;i<v.length;i++)v[i]=es[i].getText().trim();trabajar(()->"Creado P"+controlador.crearProcesoES(v[0],Integer.parseInt(v[1]),Integer.parseInt(v[2]),Integer.parseInt(v[3]),Integer.parseInt(v[4]),Integer.parseInt(v[5]),Integer.parseInt(v[6]),Integer.parseInt(v[7])).getId());});
  operaciones.add(esPanel,BorderLayout.CENTER);
  operaciones.add(new JLabel("Cambios por equipo; EDF y prioridades apropian en el siguiente ciclo. Empates estables."),BorderLayout.SOUTH);
  tabs.addTab("Politicas y E/S",new JScrollPane(operaciones));
  JSplitPane comparacion=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(metricas),grafica);comparacion.setResizeWeight(0.55);tabs.addTab("Metricas y grafica",comparacion);
  tabs.addTab("Log global",new JScrollPane(log));
  add(tabs,BorderLayout.CENTER);add(resultado,BorderLayout.SOUTH);
 }
 private static JTextArea texto(){JTextArea a=new JTextArea(12,60);a.setEditable(false);a.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));((javax.swing.text.DefaultCaret)a.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);return a;}
 private void trabajar(java.util.concurrent.Callable<String> accion){Thread t=new Thread(()->{String mensaje;try{mensaje=accion.call();}catch(NumberFormatException e){mensaje="Todos los campos numericos deben ser enteros dentro de rango.";}catch(Exception e){mensaje=e.getMessage();}String m=mensaje;SwingUtilities.invokeLater(()->resultado.setText(m));},"Operacion-integrante3");t.setDaemon(true);t.start();}
 @Override public void addNotify(){super.addNotify();timer.start();actualizar();}
 @Override public void removeNotify(){timer.stop();super.removeNotify();}
 public void actualizar(){if(!lectura.tryAcquire())return;Thread t=new Thread(()->{
  try {VistaMetricas[] vistas=controlador.consultarMetricas();EventoSistema[] eventos=controlador.consultarLog();VistaObservacionBuffers buffers=controlador.consultarObservacionBuffers();
   StringBuilder m=new StringBuilder("Utilizacion acumulada incluye usuario y operaciones de sistema; sin costo de cambio de contexto.\n");
   long ciclos=0,ocupados=0;int completos=0,vencidos=0,cancelados=0,creados=0,respondidos=0;double respuesta=0,servicio=0,cuadrados=0;
   for(VistaMetricas v:vistas){m.append(String.format(java.util.Locale.ROOT,"C%d  ciclos=%d  uso=%.2f%%  throughput=%.4f proc/ciclo\n    completados=%d vencidos=%d cancelados=%d respuesta=%.2f (%d respondidos)\n    cumplimiento=%.2f%% equidad Jain=%.4f\n",v.computador(),v.ciclos(),v.utilizacion(),v.throughput(),v.completados(),v.vencidos(),v.cancelados(),v.respuesta(),v.respondidos(),v.cumplimiento(),v.equidad()));ciclos=Math.max(ciclos,v.ciclos());ocupados+=v.ocupados();completos+=v.completados();vencidos+=v.vencidos();cancelados+=v.cancelados();creados+=v.creados();respondidos+=v.respondidos();respuesta+=v.respuesta()*v.respondidos();servicio+=v.servicio();cuadrados+=v.servicioCuadrado();}
   m.append(String.format(java.util.Locale.ROOT,"\nGLOBAL completados=%d vencidos=%d cancelados=%d\nUso=%.2f%% throughput=%.4f proc/ciclo respuesta=%.2f cumplimiento=%.2f%% Jain=%.4f\n",completos,vencidos,cancelados,ciclos==0?0:100.0*ocupados/Math.max(1,ciclos*vistas.length),ciclos==0?0:(double)completos/ciclos,respondidos==0?0:respuesta/respondidos,completos+vencidos==0?0:100.0*completos/(completos+vencidos),cuadrados==0?0:servicio*servicio/(creados*cuadrados)));
   m.append("Sin muestras: 0. Cumplimiento excluye cancelados y procesos activos.\nEn ejecucion las capturas por equipo pueden corresponder a ciclos vecinos.\n");
   for(VistaContadoresBuffer b:buffers.contadores())m.append("Buffer ").append(b.nombre()).append(": producidos=").append(b.producidos()).append(" consumidos=").append(b.consumidos()).append('\n');
   for(VistaBloqueosBuffer b:buffers.bloqueos())m.append("Espera semaforos ").append(b.nombre()).append(": ciclos=").append(b.ciclosSemaforo()).append(" promedio=").append(b.promedioSemaforo()).append('\n');
   StringBuilder l=new StringBuilder("Ultimos 1024 eventos por computador. Orden: ciclo, computador, secuencia local.\n");for(EventoSistema e:eventos)l.append("Ciclo ").append(e.ciclo()).append(" | C").append(e.computador()).append(" #").append(e.secuencia()).append(" | P").append(e.proceso()).append(" | ").append(e.tipo()).append(" | ").append(e.detalle().replace("\n"," ").replace("\r"," ")).append('\n');
   SwingUtilities.invokeLater(()->{try{mostrar(metricas,m.toString());mostrar(log,l.toString());grafica.mostrar(vistas);}finally{lectura.release();}});
  } catch(RuntimeException e){SwingUtilities.invokeLater(()->{resultado.setText("Lectura: "+e.getMessage());lectura.release();});}
 },"Leer-metricas");t.setDaemon(true);t.start();}
 private static void mostrar(JTextArea a,String s){if(a.getText().equals(s))return;JViewport vp=(JViewport)a.getParent();Point pos=vp.getViewPosition();a.setText(s);a.setSize(Math.max(a.getWidth(),vp.getWidth()),a.getPreferredSize().height);vp.setViewPosition(new Point(pos.x,Math.min(pos.y,Math.max(0,a.getPreferredSize().height-vp.getHeight()))));}
}
