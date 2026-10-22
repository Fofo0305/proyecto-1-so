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
    private final PanelBuffer panelBuffer = new PanelBuffer();
    private final PanelProductorConsumidor panelProductorConsumidor = new PanelProductorConsumidor();
    private final PanelEstadoBuffers panelEstadoBuffers = new PanelEstadoBuffers(controlador);
    private final PanelControles panelControles = new PanelControles(controlador);
    private final PanelComputadores panelComputadores = new PanelComputadores(controlador);
    private final PanelArchivos panelArchivos = new PanelArchivos(controlador, this::mostrarCarga);
    private final JButton botonConfigurar = new JButton("Aplicar configuracion");
    private final JButton botonCrear = new JButton("Crear proceso CPU-bound");
    private final JButton botonCrearBuffer = new JButton("Crear buffer y reservar RAM");
    private final JButton botonCrearComunicante = new JButton("Crear productor o consumidor");
    private final JLabel estado = new JLabel("Configure el cluster para comenzar");
    private final PanelLatenciaRed panelLatenciaRed = new PanelLatenciaRed(controlador,
            panelConfiguracion::mostrarLatencia, estado::setText);
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
        JPanel buffers = new JPanel(new BorderLayout(8, 8));
        JPanel formularioBuffers = new JPanel(new BorderLayout());
        JPanel cabeceraBuffers = new JPanel(new BorderLayout());
        cabeceraBuffers.add(panelBuffer, BorderLayout.CENTER);
        cabeceraBuffers.add(panelLatenciaRed, BorderLayout.SOUTH);
        formularioBuffers.add(cabeceraBuffers, BorderLayout.NORTH);
        formularioBuffers.add(panelEstadoBuffers, BorderLayout.CENTER);
        buffers.add(conScroll(formularioBuffers), BorderLayout.CENTER);
        buffers.add(botonCrearBuffer, BorderLayout.SOUTH);
        botonCrearBuffer.addActionListener(evento -> crearBuffer());
        pestanas.addTab("Configuracion", configuracion);
        pestanas.addTab("Procesos", procesos);
        pestanas.addTab("Buffers", buffers);
        JPanel comunicantes = new JPanel(new BorderLayout(8,8));
        JPanel camposComunicantes = new JPanel(new BorderLayout());
        camposComunicantes.add(panelProductorConsumidor, BorderLayout.NORTH);
        comunicantes.add(conScroll(camposComunicantes), BorderLayout.CENTER);
        comunicantes.add(botonCrearComunicante, BorderLayout.SOUTH);
        botonCrearComunicante.addActionListener(evento -> crearProductorConsumidor());
        pestanas.addTab("Productor / consumidor", comunicantes);
        pestanas.addTab("Simulacion en vivo", panelComputadores);
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
        botonConfigurar.setText("Crear cluster y abrir simulacion");
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

    /** Captura el formulario en EDT y reserva la RAM fuera del hilo de Swing. */
    public void crearBuffer() {
        if (!botonCrearBuffer.isEnabled()) return;
        final String[] campos = panelBuffer.leerCampos();
        try {
            PanelBuffer.validarCampos(campos);
        } catch (IllegalArgumentException error) {
            estado.setText(error.getMessage());
            return;
        }
        botonCrearBuffer.setEnabled(false);
        estado.setText("Creando buffer...");
        Thread trabajo = new Thread(() -> {
            try {
                Modelo.Buffer<Object> creado = PanelBuffer.crearBuffer(controlador, campos);
                SwingUtilities.invokeLater(() -> {
                    estado.setText("Buffer " + creado.getNombre() + " creado en computador "
                            + creado.getAnfitrion() + " | RAM reservada: " + creado.getCapacidad());
                    panelComputadores.actualizar();
                });
            } catch (RuntimeException error) {
                SwingUtilities.invokeLater(() -> estado.setText(error.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> botonCrearBuffer.setEnabled(true));
            }
        }, "Crear-buffer");
        trabajo.setDaemon(true);
        trabajo.start();
    }

    public void crearProductorConsumidor() {
        if (!botonCrearComunicante.isEnabled()) return;
        final String[] campos = panelProductorConsumidor.leerCampos();
        try { PanelProductorConsumidor.validarCampos(campos); }
        catch (IllegalArgumentException error) { estado.setText(error.getMessage()); return; }
        botonCrearComunicante.setEnabled(false);
        estado.setText("Creando productor o consumidor...");
        Thread trabajador = new Thread(() -> {
            try {
                Modelo.Proceso proceso = PanelProductorConsumidor.crearProceso(controlador, campos);
                SwingUtilities.invokeLater(() -> {
                    estado.setText("Proceso " + proceso.getId() + " creado: " + proceso.getTipo());
                    panelComputadores.actualizar();
                });
            } catch (RuntimeException error) {
                SwingUtilities.invokeLater(() -> estado.setText(error.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> botonCrearComunicante.setEnabled(true));
            }
        }, "Crear-productor-consumidor");
        trabajador.setDaemon(true);
        trabajador.start();
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
            SwingUtilities.invokeLater(() -> {
                estado.setText(resultado);
                if (actualizarControles) {
                    panelLatenciaRed.mostrarLatencia(configuracion.getLatencia());
                    panelControles.prepararSesion(configuracion.getDuracionMs());
                    pestanas.setSelectedComponent(panelComputadores);
                    panelComputadores.actualizar();
                }
                botonConfigurar.setEnabled(true);
            });
        }, "Configurar-cluster");
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
    public PanelProductorConsumidor getPanelProductorConsumidor() { return panelProductorConsumidor; }
    public void mostrarProcesos() { pestanas.setSelectedIndex(1); }
    private void mostrarCarga(Modelo.CargaInicial carga) {
        panelConfiguracion.mostrarConfiguracion(carga.getConfiguracion());
        panelLatenciaRed.mostrarLatencia(carga.getConfiguracion().getLatencia());
        panelControles.prepararSesion(carga.getConfiguracion().getDuracionMs());
        estado.setText("Carga inicial lista. Pulse Iniciar.");
        panelComputadores.actualizar();
        mostrarSimulacion();
    }
    private void mostrarSimulacion() { pestanas.setSelectedComponent(panelComputadores); }
    public Sistema.ControladorSimulacion getControlador() { return controlador; }
    public String getEstado() { return estado.getText(); }
    public PanelControles getPanelControles() { return panelControles; }
    public PanelComputadores getPanelComputadores() { return panelComputadores; }
    public PanelArchivos getPanelArchivos() { return panelArchivos; }
    public PanelLatenciaRed getPanelLatenciaRed() { return panelLatenciaRed; }

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
