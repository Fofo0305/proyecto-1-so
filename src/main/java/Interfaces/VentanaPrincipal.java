package Interfaces;

/**
 * Ventana común para los paneles del simulador.
 * @author Adolfo
 */
public class VentanaPrincipal extends javax.swing.JFrame {
    private final Sistema.ControladorSimulacion controlador = new Sistema.ControladorSimulacion();
    private final PanelConfiguracion panelConfiguracion = new PanelConfiguracion();
    private final javax.swing.JButton botonConfigurar = new javax.swing.JButton("Aplicar configuracion");
    private final javax.swing.JLabel estado = new javax.swing.JLabel("Configure el cluster para comenzar");

    public VentanaPrincipal() {
        initComponents();
        getContentPane().removeAll();
        getContentPane().setLayout(new java.awt.BorderLayout(12, 12));
        javax.swing.JPanel contenido = new javax.swing.JPanel(new java.awt.BorderLayout(12, 12));
        contenido.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contenido.add(panelConfiguracion, java.awt.BorderLayout.CENTER);
        contenido.add(botonConfigurar, java.awt.BorderLayout.SOUTH);
        getContentPane().add(titulo, java.awt.BorderLayout.NORTH);
        getContentPane().add(contenido, java.awt.BorderLayout.CENTER);
        getContentPane().add(estado, java.awt.BorderLayout.SOUTH);
        botonConfigurar.addActionListener(evento -> aplicarConfiguracion());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent evento) {
                Thread cierre = new Thread(controlador::close, "Cerrar-simulacion");
                cierre.setDaemon(true);
                cierre.start();
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    /** Lee en EDT y construye el clúster fuera del hilo de interfaz. */
    public void aplicarConfiguracion() {
        final Modelo.Configuracion configuracion;
        try {
            configuracion = panelConfiguracion.leerConfiguracion();
        } catch (IllegalArgumentException error) {
            estado.setText(error.getMessage());
            return;
        }
        botonConfigurar.setEnabled(false);
        estado.setText("Aplicando configuracion...");
        Thread trabajo = new Thread(() -> {
            String mensaje;
            try {
                controlador.configurar(configuracion);
                mensaje = "Cluster configurado: " + configuracion.getCantidadComputadores() + " computadores, politica FCFS";
            } catch (RuntimeException error) {
                mensaje = error.getMessage();
            }
            final String resultado = mensaje;
            javax.swing.SwingUtilities.invokeLater(() -> {
                estado.setText(resultado);
                botonConfigurar.setEnabled(true);
            });
        }, "Configurar-cluster");
        trabajo.setDaemon(true);
        trabajo.start();
    }

    public PanelConfiguracion getPanelConfiguracion() { return panelConfiguracion; }
    public Sistema.ControladorSimulacion getControlador() { return controlador; }
    public String getEstado() { return estado.getText(); }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        titulo = new javax.swing.JLabel();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("AvilaOS - Simulador distribuido");
        titulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        titulo.setText("AvilaOS - Configuracion del simulador");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(titulo, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(titulo, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel titulo;
    // End of variables declaration//GEN-END:variables
}
