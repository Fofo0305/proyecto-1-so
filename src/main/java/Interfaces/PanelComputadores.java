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
        cabecera.setText("Ciclo global: " + ciclo + " | " + estado);
        StringBuilder contenido = new StringBuilder();
        for (VistaComputador vista : vistas) {
            contenido.append("COMPUTADOR ").append(vista.id()).append(" | ciclo ").append(vista.ciclo())
                    .append(" | FCFS | modo ").append(vista.modo()).append('\n');
            contenido.append("RAM usada/libre: ").append(vista.usada()).append('/').append(vista.libre()).append('\n');
            contenido.append("CPU: ").append(vista.cpu()).append("\n\nNUEVOS\n").append(vista.nuevos())
                    .append("\n\nLISTOS\n").append(vista.listos()).append("\n\nBLOQUEADOS\n").append(vista.bloqueados())
                    .append("\n\nTERMINADOS\n").append(vista.terminados()).append("\n\n----------------------------------------\n\n");
        }
        detalle.setText(vistas.length == 0 ? "Configure el cluster para observar sus computadores." : contenido.toString());
        detalle.setCaretPosition(0);
    }

    public String getDetalle() { return detalle.getText(); }
    public String getCabecera() { return cabecera.getText(); }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        cabecera = new javax.swing.JLabel();
        desplazamiento = new javax.swing.JScrollPane();
        detalle = new javax.swing.JTextArea();
        setLayout(new java.awt.BorderLayout(8, 8));
        cabecera.setText("Ciclo global: 0 | Sin configurar"); add(cabecera, java.awt.BorderLayout.NORTH);
        detalle.setEditable(false); detalle.setColumns(80); detalle.setRows(22);
        detalle.setText("Configure el cluster para observar sus computadores.");
        desplazamiento.setViewportView(detalle);
        add(desplazamiento, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel cabecera;
    private javax.swing.JScrollPane desplazamiento;
    private javax.swing.JTextArea detalle;
    // End of variables declaration//GEN-END:variables
}
