package Interfaces;

import Modelo.DefinicionProceso;
import Modelo.ParametrosBuffer;
import Modelo.Proceso;
import Modelo.TipoProceso;
import Sistema.ControladorSimulacion;

/** Solicitudes de productores y consumidores locales. @author Luciano Rojas */
public class PanelProductorConsumidor extends javax.swing.JPanel {
    public PanelProductorConsumidor() {
        initComponents();
        setBackground(new java.awt.Color(246, 248, 252));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16,20,16,20));
        formulario.setOpaque(false);
    }

    public String[] leerCampos() {
        return new String[] {campoNombre.getText().trim(), campoComputador.getText().trim(),
            campoPrioridad.getText().trim(), campoMemoria.getText().trim(), campoDeadline.getText().trim(),
            (String) campoTipo.getSelectedItem(), campoBuffer.getText().trim(),
            campoIntervalo.getText().trim(), campoElementos.getText().trim()};
    }

    public static DefinicionProceso validarCampos(String[] campos) {
        if (campos == null || campos.length != 9) throw new IllegalArgumentException("Solicitud incompleta");
        try {
            ParametrosBuffer parametros = new ParametrosBuffer(campos[6], Integer.parseInt(campos[7]), Integer.parseInt(campos[8]));
            return new DefinicionProceso(campos[0], Integer.parseInt(campos[1]), Integer.parseInt(campos[2]),
                    Integer.parseInt(campos[3]), parametros.instrucciones(), Integer.parseInt(campos[4]),
                    TipoProceso.valueOf(campos[5] == null ? "" : campos[5]), parametros);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Los campos numericos deben ser enteros dentro de rango", error);
        }
    }

    public static Proceso crearProceso(ControladorSimulacion controlador, String[] campos) {
        if (controlador == null) throw new IllegalArgumentException("El controlador es obligatorio");
        DefinicionProceso d = validarCampos(campos);
        return controlador.crearProcesoBuffer(d.nombre(), d.computador(), d.prioridad(), d.memoria(),
                d.deadline(), d.tipo(), d.parametrosBuffer().buffer(), d.parametrosBuffer().intervalo(), d.parametrosBuffer().elementos());
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        introduccion = new javax.swing.JLabel();
        formulario = new javax.swing.JPanel();
        etiquetaNombre = new javax.swing.JLabel(); campoNombre = new javax.swing.JTextField();
        etiquetaComputador = new javax.swing.JLabel(); campoComputador = new javax.swing.JTextField();
        etiquetaPrioridad = new javax.swing.JLabel(); campoPrioridad = new javax.swing.JTextField();
        etiquetaMemoria = new javax.swing.JLabel(); campoMemoria = new javax.swing.JTextField();
        etiquetaDeadline = new javax.swing.JLabel(); campoDeadline = new javax.swing.JTextField();
        etiquetaTipo = new javax.swing.JLabel(); campoTipo = new javax.swing.JComboBox<>();
        etiquetaBuffer = new javax.swing.JLabel(); campoBuffer = new javax.swing.JTextField();
        etiquetaIntervalo = new javax.swing.JLabel(); campoIntervalo = new javax.swing.JTextField();
        etiquetaElementos = new javax.swing.JLabel(); campoElementos = new javax.swing.JTextField();
        ayuda = new javax.swing.JLabel();
        setLayout(new java.awt.BorderLayout(0,12));
        introduccion.setText("<html><h2>Productor y consumidor local</h2>Crea primero el buffer en el computador que ejecutara el proceso.</html>");
        add(introduccion,java.awt.BorderLayout.NORTH);
        formulario.setLayout(new java.awt.GridLayout(9,2,16,8));
        etiquetaNombre.setText("Nombre"); formulario.add(etiquetaNombre);
        campoNombre.setText("Productor"); formulario.add(campoNombre);
        etiquetaComputador.setText("Computador (ID desde 1)"); formulario.add(etiquetaComputador);
        campoComputador.setText("1"); formulario.add(campoComputador);
        etiquetaPrioridad.setText("Prioridad (desde 0)"); formulario.add(etiquetaPrioridad);
        campoPrioridad.setText("0"); formulario.add(campoPrioridad);
        etiquetaMemoria.setText("Memoria del proceso"); formulario.add(etiquetaMemoria);
        campoMemoria.setText("4"); formulario.add(campoMemoria);
        etiquetaDeadline.setText("Plazo (ciclos globales)"); formulario.add(etiquetaDeadline);
        campoDeadline.setText("500"); formulario.add(campoDeadline);
        etiquetaTipo.setText("Tipo"); formulario.add(etiquetaTipo);
        campoTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"PRODUCTOR","CONSUMIDOR"})); formulario.add(campoTipo);
        etiquetaBuffer.setText("Nombre del buffer existente"); formulario.add(etiquetaBuffer);
        campoBuffer.setText("Datos"); formulario.add(campoBuffer);
        etiquetaIntervalo.setText("Trabajo entre operaciones (ciclos CPU)"); formulario.add(etiquetaIntervalo);
        campoIntervalo.setText("1"); formulario.add(campoIntervalo);
        etiquetaElementos.setText("Cantidad de elementos objetivo"); formulario.add(etiquetaElementos);
        campoElementos.setText("3"); formulario.add(campoElementos);
        add(formulario,java.awt.BorderLayout.CENTER);
        ayuda.setText("<html>Cada elemento requiere el trabajo indicado y cinco instrucciones de sistema.<br>Si falta RAM espera en Nuevos; si falta un recurso libera la CPU.</html>");
        add(ayuda,java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel introduccion, ayuda;
    private javax.swing.JPanel formulario;
    private javax.swing.JLabel etiquetaNombre, etiquetaComputador, etiquetaPrioridad, etiquetaMemoria, etiquetaDeadline;
    private javax.swing.JLabel etiquetaTipo, etiquetaBuffer, etiquetaIntervalo, etiquetaElementos;
    private javax.swing.JTextField campoNombre, campoComputador, campoPrioridad, campoMemoria, campoDeadline;
    private javax.swing.JTextField campoBuffer, campoIntervalo, campoElementos;
    private javax.swing.JComboBox<String> campoTipo;
    // End of variables declaration//GEN-END:variables
}
