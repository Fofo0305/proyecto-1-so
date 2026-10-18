package Interfaces;

import Modelo.Proceso;
import Sistema.ControladorSimulacion;

/**
 * Campos comunes para crear procesos CPU-bound con asignación manual.
 * @author Adolfo
 */
public class PanelProceso extends javax.swing.JPanel {
    public PanelProceso() { initComponents(); }

    public Proceso crearProceso(ControladorSimulacion controlador) {
        return crearProceso(controlador, leerCampos());
    }

    /** Captura los textos en EDT para procesarlos en un trabajador. */
    public String[] leerCampos() {
        return new String[] {campoNombre.getText().trim(), campoComputador.getText().trim(),
            campoPrioridad.getText().trim(), campoMemoria.getText().trim(),
            campoInstrucciones.getText().trim(), campoDeadline.getText().trim()};
    }

    public static Proceso crearProceso(ControladorSimulacion controlador, String[] campos) {
        if (controlador == null) throw new IllegalArgumentException("El controlador es obligatorio");
        if (campos == null || campos.length != 6) throw new IllegalArgumentException("Solicitud incompleta");
        try {
            return controlador.crearProceso(campos[0], Integer.parseInt(campos[1]), Integer.parseInt(campos[2]),
                    Integer.parseInt(campos[3]), Integer.parseInt(campos[4]), Integer.parseInt(campos[5]));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Los atributos numericos del proceso deben ser enteros dentro de rango", error);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        setLayout(new java.awt.BorderLayout(0, 12));
        setBackground(new java.awt.Color(246, 248, 252));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 16, 20));
        introduccion = new javax.swing.JLabel("<html><span style='font-size:20px'>Crea un proceso</span><br>Elige su computador y sus recursos.<br>Si no hay RAM libre, esperara en Nuevos hasta poder entrar.</html>");
        introduccion.setForeground(new java.awt.Color(30, 49, 77));
        add(introduccion, java.awt.BorderLayout.NORTH);
        formulario = new javax.swing.JPanel(new java.awt.GridBagLayout()); formulario.setOpaque(false);
        java.awt.GridBagConstraints c;
        etiquetaNombre = new javax.swing.JLabel("<html><b>Nombre</b><br><span style='color:#637089'>Identifica la tarea</span></html>"); campoNombre = new javax.swing.JTextField("Proceso", 12);
        campoNombre.setName("Nombre"); etiquetaNombre.setLabelFor(campoNombre);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=0; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=0; c.fill=0; formulario.add(etiquetaNombre,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=0; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=1; c.fill=2; formulario.add(campoNombre,c);
        etiquetaComputador = new javax.swing.JLabel("<html><b>Computador</b><br><span style='color:#637089'>ID del equipo, desde 1</span></html>"); campoComputador = new javax.swing.JTextField("1", 12);
        campoComputador.setName("Computador"); etiquetaComputador.setLabelFor(campoComputador);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=1; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=0; c.fill=0; formulario.add(etiquetaComputador,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=1; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=1; c.fill=2; formulario.add(campoComputador,c);
        etiquetaInstrucciones = new javax.swing.JLabel("<html><b>Instrucciones</b><br><span style='color:#637089'>Cantidad de ciclos de CPU</span></html>"); campoInstrucciones = new javax.swing.JTextField("10", 12);
        campoInstrucciones.setName("Instrucciones"); etiquetaInstrucciones.setLabelFor(campoInstrucciones);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=2; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=0; c.fill=0; formulario.add(etiquetaInstrucciones,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=2; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=1; c.fill=2; formulario.add(campoInstrucciones,c);
        etiquetaMemoria = new javax.swing.JLabel("<html><b>Memoria</b><br><span style='color:#637089'>Unidades de RAM necesarias</span></html>"); campoMemoria = new javax.swing.JTextField("32", 12);
        campoMemoria.setName("Memoria"); etiquetaMemoria.setLabelFor(campoMemoria);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=3; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=0; c.fill=0; formulario.add(etiquetaMemoria,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=3; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=1; c.fill=2; formulario.add(campoMemoria,c);
        etiquetaPrioridad = new javax.swing.JLabel("<html><b>Prioridad</b><br><span style='color:#637089'>Entero desde 0</span></html>"); campoPrioridad = new javax.swing.JTextField("0", 12);
        campoPrioridad.setName("Prioridad"); etiquetaPrioridad.setLabelFor(campoPrioridad);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=4; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=0; c.fill=0; formulario.add(etiquetaPrioridad,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=4; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=1; c.fill=2; formulario.add(campoPrioridad,c);
        etiquetaDeadline = new javax.swing.JLabel("<html><b>Plazo (deadline)</b><br><span style='color:#637089'>Ciclos disponibles desde su llegada</span></html>"); campoDeadline = new javax.swing.JTextField("50", 12);
        campoDeadline.setName("Deadline"); etiquetaDeadline.setLabelFor(campoDeadline);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=5; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=0; c.fill=0; formulario.add(etiquetaDeadline,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=5; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,8,18); c.weightx=1; c.fill=2; formulario.add(campoDeadline,c);        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=6; c.weighty=1; formulario.add(javax.swing.Box.createVerticalGlue(),c);
        add(formulario, java.awt.BorderLayout.CENTER);
        for (javax.swing.JTextField campo : new javax.swing.JTextField[] {campoNombre, campoComputador, campoInstrucciones, campoMemoria, campoPrioridad, campoDeadline}) {
            campo.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
            campo.setMargin(new java.awt.Insets(4, 8, 4, 8));
        }
        tipo = new javax.swing.JLabel("Tipo disponible: CPU-bound · ejecuta instrucciones en la CPU");
        tipo.setForeground(new java.awt.Color(82, 101, 127));
        add(tipo, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel formulario;
    private javax.swing.JLabel introduccion, tipo;
    private javax.swing.JLabel etiquetaNombre, etiquetaComputador, etiquetaInstrucciones, etiquetaMemoria, etiquetaPrioridad, etiquetaDeadline;
    private javax.swing.JTextField campoNombre, campoComputador, campoInstrucciones, campoMemoria, campoPrioridad, campoDeadline;
    // End of variables declaration//GEN-END:variables
}