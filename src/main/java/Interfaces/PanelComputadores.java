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
    private VistaComputador[] ultimasVistas = new VistaComputador[0];
    private boolean ajustandoSelector;

    public PanelComputadores() { this(null); }

    public PanelComputadores(ControladorSimulacion controlador) {
        this.controlador = controlador;
        initComponents();
        selector.addActionListener(evento -> { if (!ajustandoSelector) aplicarFiltro(); });
        detalles.addActionListener(evento -> aplicarFiltro());
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
        ultimasVistas = vistas.clone();
        cabecera.setText("Ciclo global: " + ciclo + " | " + estado);
        int nuevos = 0, listos = 0, activos = 0, terminados = 0;
        long ramUsada = 0, ramTotal = 0;
        for (VistaComputador vista : vistas) {
            nuevos += vista.colaNuevos().length; listos += vista.colaListos().length;
            activos += vista.procesoCPU() == null ? 0 : 1; terminados += vista.colaTerminados().length;
            ramUsada += vista.usada(); ramTotal += (long) vista.usada() + vista.libre();
        }
        resumen.setText(vistas.length == 0 ? "Comienza en Configuracion: carga el ejemplo o prepara tu cluster."
                : "Cluster · " + vistas.length + " equipos    |    CPU activas " + activos
                + "    |    Esperan RAM " + nuevos + "    |    Listos " + listos
                + "    |    Terminados " + terminados + "    |    RAM " + ramUsada + "/" + ramTotal);
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
            if (vistas.length == 0) detalle.add(crearBienvenida());
        }
        if (selector.getItemCount() != vistas.length + 1) {
            ajustandoSelector = true;
            selector.removeAllItems(); selector.addItem("Todos los computadores");
            for (VistaComputador vista : vistas) selector.addItem("Computador " + vista.id());
            ajustandoSelector = false;
        }
        aplicarFiltro();
        detalle.revalidate(); detalle.repaint();
    }

    private void aplicarFiltro() {
        int elegido = selector.getSelectedIndex();
        for (int i = 0; i < paneles.length; i++) {
            boolean visible = elegido <= 0 || elegido == i + 1;
            paneles[i].setVisible(visible);
            detalle.getComponent(i * 2 + 1).setVisible(visible);
            if (visible) {
                paneles[i].mostrarDetalles(detalles.isSelected());
                paneles[i].mostrar(ultimasVistas[i]);
            }
        }
        detalle.revalidate(); detalle.repaint();
    }

    public void seleccionarComputador(int indice) { selector.setSelectedIndex(indice); }
    public void mostrarDetallesPCB(boolean valor) { detalles.setSelected(valor); aplicarFiltro(); }
    public int getComputadoresVisibles() {
        int cantidad = 0;
        for (PanelComputador panel : paneles) if (panel.isVisible()) cantidad++;
        return cantidad;
    }

    public String getDetalle() { return textoDetalle; }
    public String getCabecera() { return cabecera.getText(); }
    public String getResumen() { return resumen.getText(); }

    private javax.swing.JLabel crearBienvenida() {
        javax.swing.JLabel mensaje = new javax.swing.JLabel("<html><h1>Tu cluster, de un vistazo</h1>"
                + "1. Abre Configuracion y pulsa Cargar ejemplo.<br><br>"
                + "2. Pulsa Iniciar para ver avanzar los procesos.<br><br>"
                + "3. Usa Pausar para explorar las colas y los detalles.<br><br>"
                + "Cada computador mostrara su CPU, memoria y procesos en tiempo real.</html>");
        mensaje.setForeground(new java.awt.Color(64, 83, 111));
        mensaje.setBorder(javax.swing.BorderFactory.createEmptyBorder(48, 32, 48, 32));
        return mensaje;
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        cabecera = new javax.swing.JLabel();
        desplazamiento = new javax.swing.JScrollPane();
        detalle = new javax.swing.JPanel();
        herramientas = new javax.swing.JPanel(new java.awt.BorderLayout(12, 8));
        opciones = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        selector = new javax.swing.JComboBox<>();
        detalles = new javax.swing.JCheckBox("Detalles PCB");
        resumen = new javax.swing.JLabel("Comienza en Configuracion: carga el ejemplo o prepara tu cluster.");
        resumen.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 8, 0));
        resumen.setForeground(new java.awt.Color(82, 101, 127));
        selector.addItem("Todos los computadores");
        selector.setToolTipText("Enfocar un computador o ver el cluster completo");
        opciones.add(selector); opciones.add(detalles);
        herramientas.add(cabecera, java.awt.BorderLayout.CENTER);
        herramientas.add(opciones, java.awt.BorderLayout.EAST);
        herramientas.add(resumen, java.awt.BorderLayout.SOUTH);
        herramientas.setOpaque(false); opciones.setOpaque(false); detalles.setOpaque(false);
        setLayout(new java.awt.BorderLayout(8, 8));
        cabecera.setText("Ciclo global: 0 | Sin configurar"); add(herramientas, java.awt.BorderLayout.NORTH);
        setPreferredSize(new java.awt.Dimension(1140, 640));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(new java.awt.Color(242, 246, 251));
        cabecera.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
        cabecera.setForeground(new java.awt.Color(26, 46, 75));
        detalle.setLayout(new javax.swing.BoxLayout(detalle, javax.swing.BoxLayout.Y_AXIS));
        detalle.setBackground(new java.awt.Color(242, 246, 251));
        detalle.add(crearBienvenida());
        desplazamiento.setBorder(null);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(24);
        desplazamiento.setViewportView(detalle);
        add(desplazamiento, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel cabecera;
    private javax.swing.JScrollPane desplazamiento;
    private javax.swing.JPanel detalle;
    private javax.swing.JPanel herramientas;
    private javax.swing.JPanel opciones;
    private javax.swing.JComboBox<String> selector;
    private javax.swing.JCheckBox detalles;
    private javax.swing.JLabel resumen;
    // End of variables declaration//GEN-END:variables
}
