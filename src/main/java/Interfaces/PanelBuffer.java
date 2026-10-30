package Interfaces;

import Modelo.DefinicionBuffer;
import Modelo.Buffer;
import Sistema.ControladorSimulacion;

/** Campos de creacion de buffers, editables en NetBeans. @author Luciano Rojas */
public class PanelBuffer extends javax.swing.JPanel {
    public PanelBuffer() {
        initComponents();
        setBackground(new java.awt.Color(246, 248, 252));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 16, 20));
        formulario.setOpaque(false);
        etiquetaNombre.setLabelFor(campoNombre);
        etiquetaCapacidad.setLabelFor(campoCapacidad);
        etiquetaAnfitrion.setLabelFor(campoAnfitrion);
        for (javax.swing.JTextField campo : new javax.swing.JTextField[] {
                campoNombre, campoCapacidad, campoAnfitrion}) {
            campo.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
            campo.setMargin(new java.awt.Insets(4, 8, 4, 8));
        }
    }

    /** Captura los textos en EDT; la creacion se solicita desde un trabajador. */
    public String[] leerCampos() {
        return new String[] {campoNombre.getText().trim(), campoCapacidad.getText().trim(),
            campoAnfitrion.getText().trim()};
    }

    /** Valida formato y rango sin acceder a la simulacion ni modificar sus recursos. */
    public static DefinicionBuffer validarCampos(String[] campos) {
        if (campos == null || campos.length != 3)
            throw new IllegalArgumentException("Solicitud de buffer incompleta");
        if (campos[0] == null || campos[0].isBlank())
            throw new IllegalArgumentException("Indique el nombre del buffer");
        return new DefinicionBuffer(campos[0].trim(), enteroPositivo(campos[1], "La capacidad"),
                enteroPositivo(campos[2], "El computador anfitrion"));
    }

    private static int enteroPositivo(String texto, String atributo) {
        final int valor;
        try {
            valor = Integer.parseInt(texto == null ? "" : texto.trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(atributo + " debe ser un entero entre 1 y 2147483647", error);
        }
        if (valor < 1) throw new IllegalArgumentException(atributo + " debe ser mayor que cero");
        return valor;
    }

    public static Buffer<Object> crearBuffer(ControladorSimulacion controlador, String[] campos) {
        if (controlador == null) throw new IllegalArgumentException("El controlador es obligatorio");
        DefinicionBuffer definicion = validarCampos(campos);
        return controlador.crearBuffer(definicion.nombre(), definicion.capacidad(), definicion.anfitrion());
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        introduccion = new javax.swing.JLabel();
        formulario = new javax.swing.JPanel();
        etiquetaNombre = new javax.swing.JLabel();
        campoNombre = new javax.swing.JTextField();
        etiquetaCapacidad = new javax.swing.JLabel();
        campoCapacidad = new javax.swing.JTextField();
        etiquetaAnfitrion = new javax.swing.JLabel();
        campoAnfitrion = new javax.swing.JTextField();
        ayuda = new javax.swing.JLabel();
        setLayout(new java.awt.BorderLayout(0, 16));
        introduccion.setText("<html><h2>Crea un buffer compartido</h2>Elige su capacidad y el computador que reserva la memoria.</html>");
        add(introduccion, java.awt.BorderLayout.NORTH);
        formulario.setLayout(new java.awt.GridLayout(3, 2, 16, 12));
        etiquetaNombre.setText("Nombre unico del buffer");
        formulario.add(etiquetaNombre);
        campoNombre.setText("Datos");
        formulario.add(campoNombre);
        etiquetaCapacidad.setText("Capacidad (unidades de RAM)");
        formulario.add(etiquetaCapacidad);
        campoCapacidad.setText("8");
        formulario.add(campoCapacidad);
        etiquetaAnfitrion.setText("Computador anfitrion (ID desde 1)");
        formulario.add(etiquetaAnfitrion);
        campoAnfitrion.setText("1");
        formulario.add(campoAnfitrion);
        add(formulario, java.awt.BorderLayout.CENTER);
        ayuda.setText("<html>Configura primero el cluster. Cada posicion reserva una unidad de RAM en el anfitrion.<br>La reserva permanece aunque el buffer este vacio.</html>");
        add(ayuda, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel ayuda;
    private javax.swing.JTextField campoAnfitrion;
    private javax.swing.JTextField campoCapacidad;
    private javax.swing.JTextField campoNombre;
    private javax.swing.JLabel etiquetaAnfitrion;
    private javax.swing.JLabel etiquetaCapacidad;
    private javax.swing.JLabel etiquetaNombre;
    private javax.swing.JPanel formulario;
    private javax.swing.JLabel introduccion;
    // End of variables declaration//GEN-END:variables
}
