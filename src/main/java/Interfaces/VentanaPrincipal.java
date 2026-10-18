package Interfaces;

/**
 * Ventana común para los paneles del simulador.
 * @author Adolfo
 */
public class VentanaPrincipal extends javax.swing.JFrame {
    private final Sistema.ControladorSimulacion controlador = new Sistema.ControladorSimulacion();
    private final PanelConfiguracion panelConfiguracion = new PanelConfiguracion();
    private final PanelProceso panelProceso = new PanelProceso();
    private final PanelControles panelControles = new PanelControles(controlador);
    private final PanelComputadores panelComputadores = new PanelComputadores(controlador);
    private final PanelArchivos panelArchivos = new PanelArchivos(controlador, carga -> {
        panelConfiguracion.mostrarConfiguracion(carga.getConfiguracion());
        panelControles.mostrarDuracion(carga.getConfiguracion().getDuracionMs());
        panelComputadores.actualizar();
        mostrarSimulacion();
    });
    private final javax.swing.JButton botonConfigurar = new javax.swing.JButton("Aplicar configuracion");
    private final javax.swing.JLabel estado = new javax.swing.JLabel("Configure el cluster para comenzar");
    private final javax.swing.JTabbedPane pestanas = new javax.swing.JTabbedPane();

    public VentanaPrincipal() {
        initComponents();
        getContentPane().removeAll();
        getContentPane().setLayout(new java.awt.BorderLayout(12, 12));
        javax.swing.JPanel contenido = new javax.swing.JPanel(new java.awt.BorderLayout(12, 12));
        contenido.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        javax.swing.JPanel configuracion = new javax.swing.JPanel(new java.awt.BorderLayout(8, 8));
        javax.swing.JScrollPane desplazamientoConfig = new javax.swing.JScrollPane(panelConfiguracion);
        desplazamientoConfig.setBorder(null);
        desplazamientoConfig.getVerticalScrollBar().setUnitIncrement(20);
        configuracion.add(desplazamientoConfig, java.awt.BorderLayout.CENTER);
        configuracion.add(botonConfigurar, java.awt.BorderLayout.SOUTH);
        configuracion.add(panelArchivos, java.awt.BorderLayout.NORTH);
        javax.swing.JPanel procesos = new javax.swing.JPanel(new java.awt.BorderLayout(8, 8));
        procesos.add(panelProceso, java.awt.BorderLayout.CENTER);
        javax.swing.JButton botonCrear = new javax.swing.JButton("Crear proceso CPU-bound");
        procesos.add(botonCrear, java.awt.BorderLayout.SOUTH);
        botonCrear.addActionListener(evento -> {
            botonCrear.setEnabled(false);
            final String[] campos = panelProceso.leerCampos();
            // Se leen los campos en EDT; el acceso al núcleo se traslada a un trabajador.
            Thread trabajo = new Thread(() -> {
                try {
                    Modelo.Proceso creado = PanelProceso.crearProceso(controlador, campos);
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        estado.setText("Proceso " + creado.getId() + " creado: " + creado.getEstado());
                        pestanas.setSelectedComponent(panelComputadores);
                        panelComputadores.actualizar();
                    });
                } catch (Exception error) {
                    Throwable causa = error instanceof java.lang.reflect.InvocationTargetException ? error.getCause() : error;
                    javax.swing.SwingUtilities.invokeLater(() -> estado.setText(causa.getMessage()));
                } finally {
                    javax.swing.SwingUtilities.invokeLater(() -> botonCrear.setEnabled(true));
                }
            }, "Crear-proceso");
            trabajo.setDaemon(true);
            trabajo.start();
        });
        pestanas.addTab("Configuracion", configuracion);
        pestanas.addTab("Procesos", procesos);
        pestanas.addTab("Simulacion en vivo", panelComputadores);
        contenido.add(pestanas, java.awt.BorderLayout.CENTER);
        getContentPane().add(titulo, java.awt.BorderLayout.NORTH);
        getContentPane().add(contenido, java.awt.BorderLayout.CENTER);
        javax.swing.JPanel pie = new javax.swing.JPanel(new java.awt.BorderLayout());
        pie.add(panelControles, java.awt.BorderLayout.NORTH);
        pie.add(estado, java.awt.BorderLayout.SOUTH);
        getContentPane().add(pie, java.awt.BorderLayout.SOUTH);
        titulo.setText("AvilaOS  /  Simulador de procesos");
        titulo.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 20));
        titulo.setForeground(new java.awt.Color(30, 49, 77));
        titulo.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 16, 4, 16));
        botonConfigurar.setText("Crear cluster y abrir simulacion");
        botonConfigurar.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        botonConfigurar.setMargin(new java.awt.Insets(10, 16, 10, 16));
        botonConfigurar.setToolTipText("Reemplaza la sesion actual con los datos del formulario");
        estado.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 16, 10, 16));
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
            boolean aplicada = false;
            try {
                controlador.configurar(configuracion);
                aplicada = true;
                mensaje = "Cluster configurado: " + configuracion.getCantidadComputadores() + " computadores, politica FCFS";
            } catch (RuntimeException error) {
                mensaje = error.getMessage();
            }
            final String resultado = mensaje;
            final boolean actualizarControles = aplicada;
            javax.swing.SwingUtilities.invokeLater(() -> {
                estado.setText(resultado);
                if (actualizarControles) {
                    panelControles.mostrarDuracion(configuracion.getDuracionMs());
                    pestanas.setSelectedComponent(panelComputadores);
                    panelComputadores.actualizar();
                }
                botonConfigurar.setEnabled(true);
            });
        }, "Configurar-cluster");
        trabajo.setDaemon(true);
        trabajo.start();
    }

    public PanelConfiguracion getPanelConfiguracion() { return panelConfiguracion; }
    private void mostrarSimulacion() { pestanas.setSelectedComponent(panelComputadores); }
    public Sistema.ControladorSimulacion getControlador() { return controlador; }
    public String getEstado() { return estado.getText(); }
    public PanelControles getPanelControles() { return panelControles; }
    public PanelComputadores getPanelComputadores() { return panelComputadores; }
    public PanelArchivos getPanelArchivos() { return panelArchivos; }

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
