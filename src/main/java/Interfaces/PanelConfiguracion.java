package Interfaces;

import Modelo.Configuracion;

/**
 * Entrada de parámetros iniciales del clúster.
 * @author Adolfo
 */
public class PanelConfiguracion extends javax.swing.JPanel {
    public PanelConfiguracion() { initComponents(); }

    /** Lee los campos y valida su formato antes de construir la configuración. */
    public Configuracion leerConfiguracion() {
        try {
            int cantidad = Integer.parseInt(campoCantidad.getText().trim());
            String[] valores = campoRam.getText().split(",", -1);
            if (cantidad != valores.length) throw new IllegalArgumentException("Indique una RAM por computador, separada por comas");
            int[] ram = new int[valores.length];
            for (int i = 0; i < ram.length; i++) ram[i] = Integer.parseInt(valores[i].trim());
            return new Configuracion(ram, Integer.parseInt(campoCiclo.getText().trim()),
                    Integer.parseInt(campoQuantum.getText().trim()), Integer.parseInt(campoLatencia.getText().trim()));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Los parametros deben ser numeros enteros dentro de rango", error);
        }
    }

    /** Carga una configuración para editarla o reconstruir una simulación. */
    public void mostrarConfiguracion(Configuracion configuracion) {
        if (configuracion == null) throw new IllegalArgumentException("La configuracion es obligatoria");
        campoCantidad.setText(Integer.toString(configuracion.getCantidadComputadores()));
        int[] ram = configuracion.getRam();
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < ram.length; i++) {
            if (i > 0) texto.append(',');
            texto.append(ram[i]);
        }
        campoRam.setText(texto.toString());
        campoCiclo.setText(Integer.toString(configuracion.getDuracionMs()));
        campoQuantum.setText(Integer.toString(configuracion.getQuantum()));
        campoLatencia.setText(Integer.toString(configuracion.getLatencia()));
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        etiquetaCantidad = new javax.swing.JLabel();
        campoCantidad = new javax.swing.JTextField();
        etiquetaRam = new javax.swing.JLabel();
        campoRam = new javax.swing.JTextField();
        etiquetaCiclo = new javax.swing.JLabel();
        campoCiclo = new javax.swing.JTextField();
        etiquetaQuantum = new javax.swing.JLabel();
        campoQuantum = new javax.swing.JTextField();
        etiquetaLatencia = new javax.swing.JLabel();
        campoLatencia = new javax.swing.JTextField();
        etiquetaPolitica = new javax.swing.JLabel();
        valorPolitica = new javax.swing.JLabel();
        setLayout(new java.awt.GridLayout(6, 2, 8, 8));
        etiquetaCantidad.setText("Computadores (minimo 2)"); add(etiquetaCantidad);
        campoCantidad.setText("2"); add(campoCantidad);
        etiquetaRam.setText("RAM por computador, separada por comas"); add(etiquetaRam);
        campoRam.setText("128,128"); add(campoRam);
        etiquetaCiclo.setText("Duracion del ciclo (1-60000 ms)"); add(etiquetaCiclo);
        campoCiclo.setText("100"); add(campoCiclo);
        etiquetaQuantum.setText("Quantum inicial (para Round Robin)"); add(etiquetaQuantum);
        campoQuantum.setText("4"); add(campoQuantum);
        etiquetaLatencia.setText("Latencia de red (ciclos)"); add(etiquetaLatencia);
        campoLatencia.setText("0"); add(campoLatencia);
        etiquetaPolitica.setText("Politica inicial disponible"); add(etiquetaPolitica);
        valorPolitica.setText("FCFS"); add(valorPolitica);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField campoCantidad;
    private javax.swing.JTextField campoRam;
    private javax.swing.JTextField campoCiclo;
    private javax.swing.JTextField campoQuantum;
    private javax.swing.JTextField campoLatencia;
    private javax.swing.JLabel etiquetaCantidad;
    private javax.swing.JLabel etiquetaRam;
    private javax.swing.JLabel etiquetaCiclo;
    private javax.swing.JLabel etiquetaQuantum;
    private javax.swing.JLabel etiquetaLatencia;
    private javax.swing.JLabel etiquetaPolitica;
    private javax.swing.JLabel valorPolitica;
    // End of variables declaration//GEN-END:variables
}
