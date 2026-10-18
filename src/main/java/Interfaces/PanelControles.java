package Interfaces;

import javax.swing.*;
import java.awt.*;

import Sistema.ControladorSimulacion;

/**
 * Controles del reloj. Las operaciones del núcleo se ejecutan fuera del EDT.
 * @author Adolfo
 */
public class PanelControles extends JPanel {
    private final ControladorSimulacion controlador;

    public PanelControles() { this(null); }

    public PanelControles(ControladorSimulacion controlador) {
        this.controlador = controlador;
        initComponents();
        iniciar.addActionListener(evento -> ejecutarAccion("iniciar"));
        pausar.addActionListener(evento -> ejecutarAccion("pausar"));
        reanudar.addActionListener(evento -> ejecutarAccion("reanudar"));
        detener.addActionListener(evento -> ejecutarAccion("detener"));
        aplicarCiclo.addActionListener(evento -> ejecutarAccion("ciclo"));
    }

    /** Solicita una acción sin esperar al reloj dentro del hilo de Swing. */
    public void ejecutarAccion(String accion) {
        if (controlador == null) { resultado.setText("Sin controlador"); return; }
        final String duracion = ciclo.getText().trim();
        habilitar(false);
        Thread trabajador = new Thread(() -> {
            String mensaje;
            try {
                switch (accion) {
                    case "iniciar" -> controlador.iniciar();
                    case "pausar" -> controlador.pausar();
                    case "reanudar" -> controlador.reanudar();
                    case "detener" -> controlador.detener();
                    case "ciclo" -> controlador.cambiarDuracion(Integer.parseInt(duracion));
                    default -> throw new IllegalArgumentException("Accion desconocida");
                }
                mensaje = controlador.getEstado();
            } catch (NumberFormatException error) {
                mensaje = "El ciclo debe ser un entero entre 1 y 60000 ms";
            } catch (RuntimeException error) {
                mensaje = error.getMessage();
            }
            final String texto = mensaje;
            SwingUtilities.invokeLater(() -> { resultado.setText(texto); habilitar(true); });
        }, "Control-simulacion");
        trabajador.setDaemon(true);
        trabajador.start();
    }

    private void habilitar(boolean valor) {
        iniciar.setEnabled(valor); pausar.setEnabled(valor); reanudar.setEnabled(valor);
        detener.setEnabled(valor); aplicarCiclo.setEnabled(valor);
    }

    public void mostrarDuracion(int duracionMs) { ciclo.setText(Integer.toString(duracionMs)); }
    public void prepararSesion(int duracionMs) {
        mostrarDuracion(duracionMs);
        resultado.setText("Preparado");
    }
    public String getDuracionMostrada() { return ciclo.getText(); }
    public String getResultado() { return resultado.getText(); }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        iniciar = new javax.swing.JButton(); pausar = new javax.swing.JButton();
        reanudar = new javax.swing.JButton(); detener = new javax.swing.JButton();
        ciclo = new javax.swing.JTextField(); aplicarCiclo = new javax.swing.JButton();
        resultado = new javax.swing.JLabel();
        acciones = new javax.swing.JPanel(new java.awt.GridLayout(1, 4, 8, 0));
        ajustes = new javax.swing.JPanel(new java.awt.BorderLayout(10, 0));
        velocidad = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        etiquetaCiclo = new javax.swing.JLabel("Ciclo (ms)");
        setLayout(new java.awt.BorderLayout(0, 8));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 16, 4, 16));
        iniciar.setText("Iniciar"); acciones.add(iniciar);
        pausar.setText("Pausar"); acciones.add(pausar);
        reanudar.setText("Reanudar"); acciones.add(reanudar);
        detener.setText("Detener"); acciones.add(detener);
        ciclo.setColumns(5); ciclo.setText("700"); velocidad.add(etiquetaCiclo); velocidad.add(ciclo);
        aplicarCiclo.setText("Aplicar"); velocidad.add(aplicarCiclo);
        resultado.setText("Preparado"); ajustes.add(resultado, java.awt.BorderLayout.CENTER);
        ajustes.add(velocidad, java.awt.BorderLayout.WEST);
        add(acciones, java.awt.BorderLayout.NORTH); add(ajustes, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton iniciar;
    private javax.swing.JButton pausar;
    private javax.swing.JButton reanudar;
    private javax.swing.JButton detener;
    private javax.swing.JTextField ciclo;
    private javax.swing.JButton aplicarCiclo;
    private javax.swing.JLabel resultado;
    private javax.swing.JPanel acciones, ajustes, velocidad;
    private javax.swing.JLabel etiquetaCiclo;
    // End of variables declaration//GEN-END:variables
}
