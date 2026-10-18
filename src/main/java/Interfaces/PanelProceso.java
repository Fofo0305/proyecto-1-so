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
        etiquetaNombre = new javax.swing.JLabel(); campoNombre = new javax.swing.JTextField();
        etiquetaComputador = new javax.swing.JLabel(); campoComputador = new javax.swing.JTextField();
        etiquetaInstrucciones = new javax.swing.JLabel(); campoInstrucciones = new javax.swing.JTextField();
        etiquetaMemoria = new javax.swing.JLabel(); campoMemoria = new javax.swing.JTextField();
        etiquetaPrioridad = new javax.swing.JLabel(); campoPrioridad = new javax.swing.JTextField();
        etiquetaDeadline = new javax.swing.JLabel(); campoDeadline = new javax.swing.JTextField();
        setLayout(new java.awt.GridLayout(6, 2, 8, 8));
        etiquetaNombre.setText("Nombre (tipo CPU-bound)"); add(etiquetaNombre);
        campoNombre.setText("Proceso"); add(campoNombre);
        etiquetaComputador.setText("Computador asignado (ID desde 1)"); add(etiquetaComputador);
        campoComputador.setText("1"); add(campoComputador);
        etiquetaInstrucciones.setText("Instrucciones (positivo)"); add(etiquetaInstrucciones);
        campoInstrucciones.setText("10"); add(campoInstrucciones);
        etiquetaMemoria.setText("Memoria requerida (unidades de RAM)"); add(etiquetaMemoria);
        campoMemoria.setText("32"); add(campoMemoria);
        etiquetaPrioridad.setText("Prioridad (0 o mayor)"); add(etiquetaPrioridad);
        campoPrioridad.setText("0"); add(campoPrioridad);
        etiquetaDeadline.setText("Deadline relativo (ciclos positivos)"); add(etiquetaDeadline);
        campoDeadline.setText("50"); add(campoDeadline);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel etiquetaNombre;
    private javax.swing.JLabel etiquetaComputador;
    private javax.swing.JLabel etiquetaInstrucciones;
    private javax.swing.JLabel etiquetaMemoria;
    private javax.swing.JLabel etiquetaPrioridad;
    private javax.swing.JLabel etiquetaDeadline;
    private javax.swing.JTextField campoNombre;
    private javax.swing.JTextField campoComputador;
    private javax.swing.JTextField campoInstrucciones;
    private javax.swing.JTextField campoMemoria;
    private javax.swing.JTextField campoPrioridad;
    private javax.swing.JTextField campoDeadline;
    // End of variables declaration//GEN-END:variables
}
