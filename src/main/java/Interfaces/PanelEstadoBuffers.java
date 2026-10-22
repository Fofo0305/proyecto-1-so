package Interfaces;

import Modelo.*;
import Sistema.ControladorSimulacion;
import javax.swing.*;

/** Lecturas y formato fuera del EDT; presentacion de copias sin modificar el motor.
 * @author Luciano Rojas
 */
public final class PanelEstadoBuffers extends JPanel {
    private final ControladorSimulacion controlador;
    private final java.util.concurrent.Semaphore lectura = new java.util.concurrent.Semaphore(1);
    private final Timer refresco;
    private final JTextArea estado = area("estado-buffers");
    private final JTextArea procesos = area("procesos-buffers");
    private final JTextArea eventos = area("eventos-buffers");
    private final JScrollPane scrollEstado = new JScrollPane(estado);
    private final JScrollPane scrollProcesos = new JScrollPane(procesos);
    private final JScrollPane scrollEventos = new JScrollPane(eventos);

    public PanelEstadoBuffers() { this(null); }
    public PanelEstadoBuffers(ControladorSimulacion controlador) {
        this.controlador = controlador;
        setLayout(new java.awt.BorderLayout(0,8));
        setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        add(new JLabel("Observacion de buffers: lecturas de la sesion en curso"), java.awt.BorderLayout.NORTH);
        JTabbedPane vistas = new JTabbedPane();
        vistas.addTab("Estado y metricas", scrollEstado);
        vistas.addTab("Por proceso", scrollProcesos);
        vistas.addTab("Eventos", scrollEventos);
        add(vistas, java.awt.BorderLayout.CENTER);
        add(new JLabel("Promedio = ciclos / esperas iniciadas; incluye abiertas y canceladas."), java.awt.BorderLayout.SOUTH);
        estado.setText("Sin buffers registrados en esta sesion.");
        procesos.setText("Sin procesos productores o consumidores.");
        eventos.setText("Sin eventos registrados.");
        refresco = new Timer(200, e -> actualizar());
    }
    private static JTextArea area(String nombre) {
        JTextArea a = new JTextArea(16,60);
        a.setName(nombre);
        a.setEditable(false);
        a.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN,12));
        ((javax.swing.text.DefaultCaret)a.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        return a;
    }
    @Override public void addNotify() { super.addNotify(); refresco.start(); actualizar(); }
    @Override public void removeNotify() { refresco.stop(); super.removeNotify(); }
    public void actualizar() {
        if (controlador == null || !lectura.tryAcquire()) return;
        Thread hilo = new Thread(() -> {
            String[] textos;
            try { textos = formatear(controlador.consultarObservacionBuffers()); }
            catch (RuntimeException error) { textos = new String[]{"Error de lectura: " + error.getMessage(),"Lectura no disponible.","Lectura no disponible."}; }
            final String[] copia = textos;
            SwingUtilities.invokeLater(() -> {
                try { mostrarTextos(copia); } finally { lectura.release(); }
            });
        }, "Leer-observacion-buffers");
        hilo.setDaemon(true);
        hilo.start();
    }
    /** Presenta una copia ya capturada; util tambien para inspeccion sin reloj activo. */
    public void mostrarVista(VistaObservacionBuffers vista) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Presentar en EDT");
        mostrarTextos(formatear(vista));
    }
    private void mostrarTextos(String[] textos) {
        conservarScroll(estado,scrollEstado,textos[0]);
        conservarScroll(procesos,scrollProcesos,textos[1]);
        conservarScroll(eventos,scrollEventos,textos[2]);
    }
    private static void conservarScroll(JTextArea area,JScrollPane scroll,String texto) {
        if (area.getText().equals(texto)) return;
        java.awt.Point posicion = scroll.getViewport().getViewPosition();
        area.setText(texto);
        area.setSize(Math.max(area.getWidth(),scroll.getViewport().getWidth()),area.getPreferredSize().height);
        int maximo = Math.max(0,area.getPreferredSize().height-scroll.getViewport().getHeight());
        scroll.getViewport().setViewPosition(new java.awt.Point(posicion.x,Math.min(posicion.y,maximo)));
    }
    private static String limpio(String s) { return s.replace("\r","\\r").replace("\n","\\n"); }
    private static String decimal(double valor) { return String.format(java.util.Locale.ROOT,"%.2f",valor); }
    private static String[] formatear(VistaObservacionBuffers vista) {
        StringBuilder e = new StringBuilder(),p = new StringBuilder(),h = new StringBuilder();
        for (VistaBuffer b : vista.estados()) {
            e.append(limpio(b.nombre())).append(" | computador ").append(b.anfitrion())
                .append(" | elementos ").append(b.cantidad()).append('/').append(b.capacidad())
                .append("\nContenido: ").append(limpio(b.contenido()))
                .append("\nmutex=").append(b.mutex()).append(" | vacios=").append(b.vacios()).append(" | llenos=").append(b.llenos())
                .append("\nEspera mutex: ").append(limpio(b.esperaMutex()))
                .append("\nEspera vacios: ").append(limpio(b.esperaVacios()))
                .append("\nEspera llenos: ").append(limpio(b.esperaLlenos())).append("\n\n");
        }
        for (VistaContadoresBuffer b : vista.contadores())
            e.append(limpio(b.nombre())).append(" | Producciones: ").append(b.producidos()).append(" | Consumos: ").append(b.consumidos()).append(" | Ocupacion al contar: ").append(b.cantidad()).append('\n');
        for (VistaBloqueosBuffer b : vista.bloqueos()) {
            e.append(limpio(b.nombre())).append("\n  Semaforos: ciclos=").append(b.ciclosSemaforo()).append(" | esperas=").append(b.esperasSemaforo()).append(" | abiertas=").append(b.abiertasSemaforo()).append(" | promedio=").append(decimal(b.promedioSemaforo()))
                .append("\n  Red: ciclos=").append(b.ciclosRed()).append(" | esperas=").append(b.esperasRed()).append(" | abiertas=").append(b.abiertasRed()).append(" | promedio=").append(decimal(b.promedioRed())).append("\n\n");
        }
        for (VistaBloqueoProceso b : vista.procesos()) {
            p.append('P').append(b.proceso()).append(' ').append(limpio(b.nombre())).append(" | C").append(b.computador()).append(" | ").append(limpio(b.buffer())).append(" | espera actual: ").append(b.esperaActual())
                .append("\n  Semaforos: ciclos=").append(b.ciclosSemaforo()).append(" | esperas=").append(b.esperasSemaforo()).append(" | promedio=").append(decimal(b.promedioSemaforo()))
                .append("\n  Red: ciclos=").append(b.ciclosRed()).append(" | esperas=").append(b.esperasRed()).append(" | promedio=").append(decimal(b.promedioRed())).append("\n\n");
        }
        for (VistaEventosBuffer b : vista.eventos()) {
            h.append(limpio(b.buffer())).append(" | eventos antiguos descartados: ").append(b.descartados()).append('\n');
            for (EventoBuffer evento : b.eventos()) h.append('#').append(evento.secuencia()).append(" | ciclo ").append(evento.ciclo()).append(" | C").append(evento.computador()).append(" | P").append(evento.proceso()).append(" | ").append(evento.tipo()).append(" | ").append(limpio(evento.detalle())).append('\n');
            h.append('\n');
        }
        return new String[]{e.isEmpty()?"Sin buffers registrados en esta sesion.":e.toString(),p.isEmpty()?"Sin procesos productores o consumidores.":p.toString(),h.isEmpty()?"Sin eventos registrados.":h.toString()};
    }
    public String getTextoEstado() { return estado.getText(); }
    public String getTextoProcesos() { return procesos.getText(); }
    public String getTextoEventos() { return eventos.getText(); }
}
