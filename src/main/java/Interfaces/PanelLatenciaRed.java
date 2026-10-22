package Interfaces;

import Sistema.ControladorSimulacion;

/** Configuracion de red en una sesion activa. @author Luciano Rojas */
public class PanelLatenciaRed extends javax.swing.JPanel {
    private final ControladorSimulacion controlador;
    private final java.util.function.IntConsumer alCambiar;
    private final java.util.function.Consumer<String> mensaje;

    public PanelLatenciaRed() { this(null, valor -> { }, texto -> { }); }

    public PanelLatenciaRed(ControladorSimulacion controlador, java.util.function.IntConsumer alCambiar,
            java.util.function.Consumer<String> mensaje) {
        this.controlador = controlador;
        this.alCambiar = alCambiar;
        this.mensaje = mensaje;
        initComponents();
        setBorder(javax.swing.BorderFactory.createEmptyBorder(4,20,8,20));
        etiqueta.setLabelFor(campoLatencia);
        aplicar.addActionListener(evento -> aplicar());
    }

    public void mostrarLatencia(int latencia) { campoLatencia.setText(Integer.toString(latencia)); }

    public void aplicar() {
        if (!aplicar.isEnabled()) return;
        final int latencia;
        try {
            latencia = Integer.parseInt(campoLatencia.getText().trim());
            if (latencia < 0) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            mensaje.accept("La latencia debe ser un entero entre 0 y 2147483647");
            return;
        }
        if (controlador == null) { mensaje.accept("Configure primero el cluster"); return; }
        aplicar.setEnabled(false);
        mensaje.accept("Aplicando latencia...");
        Thread trabajador = new Thread(() -> {
            try {
                controlador.cambiarLatencia(latencia);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    alCambiar.accept(latencia);
                    mensaje.accept("Latencia aplicada: " + latencia + " ciclos para nuevos accesos remotos");
                });
            } catch (RuntimeException error) {
                javax.swing.SwingUtilities.invokeLater(() -> mensaje.accept(error.getMessage()));
            } finally {
                javax.swing.SwingUtilities.invokeLater(() -> aplicar.setEnabled(true));
            }
        }, "Cambiar-latencia-red");
        trabajador.setDaemon(true);
        trabajador.start();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        etiqueta = new javax.swing.JLabel();
        campoLatencia = new javax.swing.JTextField();
        aplicar = new javax.swing.JButton();
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT,8,4));
        etiqueta.setText("Latencia de red (ciclos)"); add(etiqueta);
        campoLatencia.setColumns(6); campoLatencia.setText("0"); add(campoLatencia);
        aplicar.setText("Aplicar latencia"); add(aplicar);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel etiqueta;
    private javax.swing.JTextField campoLatencia;
    private javax.swing.JButton aplicar;
    // End of variables declaration//GEN-END:variables
}
