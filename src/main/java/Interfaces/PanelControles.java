package Interfaces;

import Sistema.ControladorSimulacion;

/**
 * Controles del reloj. Las operaciones del núcleo se ejecutan fuera del EDT.
 * @author Adolfo
 */
public class PanelControles extends javax.swing.JPanel {
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
            javax.swing.SwingUtilities.invokeLater(() -> { resultado.setText(texto); habilitar(true); });
        }, "Control-simulacion");
        trabajador.setDaemon(true);
        trabajador.start();
    }

    private void habilitar(boolean valor) {
        iniciar.setEnabled(valor); pausar.setEnabled(valor); reanudar.setEnabled(valor);
        detener.setEnabled(valor); aplicarCiclo.setEnabled(valor);
    }

    public void mostrarDuracion(int duracionMs) { ciclo.setText(Integer.toString(duracionMs)); }
    public String getResultado() { return resultado.getText(); }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        iniciar = new javax.swing.JButton(); pausar = new javax.swing.JButton();
        reanudar = new javax.swing.JButton(); detener = new javax.swing.JButton();
        ciclo = new javax.swing.JTextField(); aplicarCiclo = new javax.swing.JButton();
        resultado = new javax.swing.JLabel();
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        iniciar.setText("Iniciar"); add(iniciar);
        pausar.setText("Pausar"); add(pausar);
        reanudar.setText("Reanudar"); add(reanudar);
        detener.setText("Detener"); add(detener);
        ciclo.setColumns(5); ciclo.setText("100"); add(ciclo);
        aplicarCiclo.setText("Aplicar ciclo (ms)"); add(aplicarCiclo);
        resultado.setText("Preparado"); add(resultado);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton iniciar;
    private javax.swing.JButton pausar;
    private javax.swing.JButton reanudar;
    private javax.swing.JButton detener;
    private javax.swing.JTextField ciclo;
    private javax.swing.JButton aplicarCiclo;
    private javax.swing.JLabel resultado;
    // End of variables declaration//GEN-END:variables
}
