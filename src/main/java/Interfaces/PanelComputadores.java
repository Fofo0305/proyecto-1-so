package Interfaces;

import Modelo.VistaComputador;
import Sistema.ControladorSimulacion;
import java.util.concurrent.Semaphore;

/**
 * Observa el núcleo sin modificarlo. Las lecturas se hacen fuera del EDT.
 * @author Adolfo
 */
public class PanelComputadores extends javax.swing.JPanel {
    private final ControladorSimulacion controlador;
    private final Semaphore lectura = new Semaphore(1);
    private final javax.swing.Timer refresco;
    private String textoDetalle = "";
    private PanelComputador[] paneles = new PanelComputador[0];

    public PanelComputadores() { this(null); }

    public PanelComputadores(ControladorSimulacion controlador) {
        this.controlador = controlador;
        initComponents();
        refresco = new javax.swing.Timer(200, evento -> actualizar());
    }

    @Override
    public void addNotify() { super.addNotify(); refresco.start(); }

    @Override
    public void removeNotify() { refresco.stop(); super.removeNotify(); }

    public void actualizar() {
        if (controlador == null || !lectura.tryAcquire()) return;
        Thread trabajador = new Thread(() -> {
            try {
                VistaComputador[] vistas = controlador.consultarVistas();
                String estado = controlador.getEstado();
                long ciclo = controlador.getCiclo();
                javax.swing.SwingUtilities.invokeLater(() -> mostrarVistas(vistas, estado, ciclo));
            } catch (RuntimeException error) {
                javax.swing.SwingUtilities.invokeLater(() -> cabecera.setText("Error de lectura: " + error.getMessage()));
            } finally { lectura.release(); }
        }, "Leer-computadores");
        trabajador.setDaemon(true);
        trabajador.start();
    }

    /** Actualiza solo componentes Swing a partir de vistas inmutables ya capturadas. */
    public void mostrarVistas(VistaComputador[] vistas, String estado, long ciclo) {
        cabecera.setText("<html>Ciclo global: " + ciclo + " | " + estado.replace("&", "&amp;").replace("<", "&lt;")
                + "<br><span style='font-size:10px;font-weight:normal'>Nuevos → RAM disponible → Listos → CPU → Terminados · Bloqueados: espera de recurso</span></html>");
        StringBuilder contenido = new StringBuilder();
        for (VistaComputador vista : vistas) {
            contenido.append("COMPUTADOR ").append(vista.id()).append(" | ciclo ").append(vista.ciclo())
                    .append(" | FCFS | modo ").append(vista.modo()).append('\n');
            contenido.append("RAM usada/libre: ").append(vista.usada()).append('/').append(vista.libre()).append('\n');
            contenido.append("CPU: ").append(vista.cpu()).append("\n\nNUEVOS\n").append(vista.nuevos())
                    .append("\n\nLISTOS\n").append(vista.listos()).append("\n\nBLOQUEADOS\n").append(vista.bloqueados())
                    .append("\n\nTERMINADOS\n").append(vista.terminados()).append("\n\n----------------------------------------\n\n");
        }
        textoDetalle = contenido.toString();
        if (paneles.length != vistas.length) {
            detalle.removeAll();
            paneles = new PanelComputador[vistas.length];
            for (int i = 0; i < vistas.length; i++) {
                paneles[i] = new PanelComputador();
                paneles[i].setAlignmentX(LEFT_ALIGNMENT);
                detalle.add(paneles[i]);
                detalle.add(javax.swing.Box.createVerticalStrut(16));
            }
            if (vistas.length == 0) detalle.add(new javax.swing.JLabel("Configure el cluster para observar CPU, RAM y colas."));
        }
        for (int i = 0; i < vistas.length; i++) paneles[i].mostrar(vistas[i]);
        detalle.revalidate(); detalle.repaint();
    }

    public String getDetalle() { return textoDetalle; }
    public String getCabecera() { return cabecera.getText(); }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        cabecera = new javax.swing.JLabel();
        desplazamiento = new javax.swing.JScrollPane();
        detalle = new javax.swing.JPanel();
        setLayout(new java.awt.BorderLayout(8, 8));
        cabecera.setText("Ciclo global: 0 | Sin configurar"); add(cabecera, java.awt.BorderLayout.NORTH);
        setPreferredSize(new java.awt.Dimension(1140, 640));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(new java.awt.Color(242, 246, 251));
        cabecera.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));
        cabecera.setForeground(new java.awt.Color(26, 46, 75));
        detalle.setLayout(new javax.swing.BoxLayout(detalle, javax.swing.BoxLayout.Y_AXIS));
        detalle.setBackground(new java.awt.Color(242, 246, 251));
        detalle.add(new javax.swing.JLabel("Configure el cluster para observar CPU, RAM y colas."));
        desplazamiento.setBorder(null);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(24);
        desplazamiento.setViewportView(detalle);
        add(desplazamiento, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel cabecera;
    private javax.swing.JScrollPane desplazamiento;
    private javax.swing.JPanel detalle;
    // End of variables declaration//GEN-END:variables
}
