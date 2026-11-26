package Interfaces;

import javax.swing.*;
import java.awt.*;

/**
 * Ventana común para los paneles del simulador.
 * @author Adolfo
 */
public class VentanaPrincipal extends JFrame {
    private final Sistema.ControladorSimulacion controlador = new Sistema.ControladorSimulacion();
    private final PanelConfiguracion panelConfiguracion = new PanelConfiguracion();
    private final PanelProceso panelProceso = new PanelProceso();
    private final PanelControles panelControles = new PanelControles(controlador);
    private final PanelComputadores panelComputadores = new PanelComputadores(controlador);
    private final PanelArchivos panelArchivos = new PanelArchivos(controlador, this::mostrarCarga);
    private final JButton botonConfigurar = new JButton("Aplicar configuracion");
    private final JButton botonCrear = new JButton("Crear proceso CPU-bound");
    private final JLabel estado = new JLabel("Configure la simulacion para comenzar");
    private final JTabbedPane pestanas = new JTabbedPane();

    public VentanaPrincipal() {
        initComponents();
        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout(12, 12));
        JPanel contenido = new JPanel(new BorderLayout(12, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel configuracion = new JPanel(new BorderLayout(8, 8));
        configuracion.add(conScroll(panelConfiguracion), BorderLayout.CENTER);
        configuracion.add(botonConfigurar, BorderLayout.SOUTH);
        configuracion.add(panelArchivos, BorderLayout.NORTH);
        JPanel procesos = new JPanel(new BorderLayout(8, 8));
        procesos.add(conScroll(panelProceso), BorderLayout.CENTER);

        procesos.add(botonCrear, BorderLayout.SOUTH);
        botonCrear.addActionListener(evento -> crearProceso());
        pestanas.addTab("Configuracion", configuracion);
        pestanas.addTab("Procesos", procesos);
        pestanas.addTab("Simulacion en vivo", panelComputadores);
        pestanas.addTab("Planificacion E/S y metricas", new PanelPlanificacion(controlador));
        pestanas.addTab("Disco y archivos", new PanelDisco(controlador));
        contenido.add(pestanas, BorderLayout.CENTER);
        getContentPane().add(titulo, BorderLayout.NORTH);
        getContentPane().add(contenido, BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout());
        pie.add(panelControles, BorderLayout.NORTH);
        pie.add(estado, BorderLayout.SOUTH);
        getContentPane().add(pie, BorderLayout.SOUTH);
        titulo.setText("AvilaOS  /  Simulador de procesos");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(new Color(30, 49, 77));
        titulo.setBorder(BorderFactory.createEmptyBorder(18, 16, 4, 16));
        botonConfigurar.setText("Crear computador y abrir simulacion");
        botonConfigurar.setFont(new Font("SansSerif", Font.BOLD, 14));
        botonConfigurar.setMargin(new Insets(10, 16, 10, 16));
        botonConfigurar.setToolTipText("Reemplaza la sesion actual con los datos del formulario");
        estado.setBorder(BorderFactory.createEmptyBorder(6, 16, 10, 16));
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
        Rectangle pantalla = getGraphicsConfiguration().getBounds();
        Insets margenes = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int anchoDisponible = pantalla.width - margenes.left - margenes.right;
        int altoDisponible = pantalla.height - margenes.top - margenes.bottom;
        setSize(Math.min(1050, Math.max(320, anchoDisponible - 64)),
                Math.min(760, Math.max(320, altoDisponible - 64)));
        setLocationRelativeTo(null);
    }

    /** Captura el formulario en EDT y solicita la creacion fuera de Swing. */
    public void crearProceso() {
        botonCrear.setEnabled(false);
        final String[] campos = panelProceso.leerCampos();
        // Se leen los campos en EDT; el acceso al núcleo se traslada a un trabajador.
        Thread trabajo = new Thread(() -> {
            try {
                Modelo.Proceso creado = PanelProceso.crearProceso(controlador, campos);
                SwingUtilities.invokeLater(() -> {
                    estado.setText("Proceso " + creado.getId() + " creado: " + creado.getEstado());
                    pestanas.setSelectedComponent(panelComputadores);
                    panelComputadores.actualizar();
                });
            } catch (RuntimeException error) {
                SwingUtilities.invokeLater(() -> estado.setText(error.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> botonCrear.setEnabled(true));
            }
        }, "Crear-proceso");
        trabajo.setDaemon(true);
        trabajo.start();

    }

    /** Lee en EDT y construye el computador fuera del hilo de interfaz. */
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
                int cantidad = configuracion.getCantidadComputadores();
                mensaje = "Simulacion configurada: " + cantidad + (cantidad == 1 ? " computador" : " computadores")
                        + ", politica FCFS";
            } catch (RuntimeException error) {
                mensaje = error.getMessage();
            }
            final String resultado = mensaje;
            final boolean actualizarControles = aplicada;
            SwingUtilities.invokeLater(() -> {
                estado.setText(resultado);
                if (actualizarControles) {
                    panelControles.prepararSesion(configuracion.getDuracionMs());
                    pestanas.setSelectedComponent(panelComputadores);
                    panelComputadores.actualizar();
                }
                panelArchivos.actualizarFaltante();
                botonConfigurar.setEnabled(true);
            });
        }, "Configurar-computador");
        trabajo.setDaemon(true);
        trabajo.start();
    }

    private JScrollPane conScroll(JPanel panel) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        return scroll;
    }
    public PanelConfiguracion getPanelConfiguracion() { return panelConfiguracion; }
    public PanelProceso getPanelProceso() { return panelProceso; }
    public void mostrarProcesos() { pestanas.setSelectedIndex(1); }
    private void mostrarCarga(Modelo.CargaInicial carga) {
        panelConfiguracion.mostrarConfiguracion(carga.getConfiguracion());
        panelControles.prepararSesion(carga.getConfiguracion().getDuracionMs());
        String falta = controlador.faltaParaIniciar();
        estado.setText(falta == null ? "Carga inicial lista. Pulse Iniciar." : "Carga lista. " + falta + ".");
        panelArchivos.actualizarFaltante();
        panelComputadores.actualizar();
        mostrarSimulacion();
    }
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
        setTitle("AvilaOS - Sistema de archivos y procesos");
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
