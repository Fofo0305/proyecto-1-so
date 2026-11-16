package Interfaces;

import Modelo.Configuracion;

/**
 * Entrada de parámetros iniciales de la simulación.
 * @author Adolfo
 */
public class PanelConfiguracion extends javax.swing.JPanel {
    public PanelConfiguracion() { initComponents(); avanzados.setVisible(false); opciones.addActionListener(evento -> { avanzados.setVisible(opciones.isSelected()); revalidate(); }); }

    /** Lee los campos y valida su formato antes de construir la configuración. */
    public Configuracion leerConfiguracion() {
        for (javax.swing.JTextField campo : new javax.swing.JTextField[] {campoRam, campoCiclo, campoQuantum})
            campo.setBorder(javax.swing.UIManager.getBorder("TextField.border"));
        int ram = leerEntero(campoRam, "RAM", 1, Integer.MAX_VALUE);
        return new Configuracion(new int[] {ram}, leerEntero(campoCiclo, "Duracion del ciclo en ms", 1, 60000),
                leerEntero(campoQuantum, "Quantum", 1, Integer.MAX_VALUE));
    }

    private int leerEntero(javax.swing.JTextField campo, String nombre, int minimo, int maximo) {
        int valor;
        try { valor = Integer.parseInt(campo.getText().trim()); }
        catch (NumberFormatException error) { throw rechazar(campo, nombre + ": escribe un numero entero"); }
        if (valor < minimo || valor > maximo)
            throw rechazar(campo, nombre + ": valor permitido entre " + minimo + " y " + maximo);
        return valor;
    }

    private IllegalArgumentException rechazar(javax.swing.JTextField campo, String mensaje) {
        campo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(190, 67, 67), 2));
        if (campo == campoQuantum) { opciones.setSelected(true); avanzados.setVisible(true); revalidate(); }
        campo.requestFocusInWindow(); campo.selectAll();
        return new IllegalArgumentException(mensaje);
    }
    /** Carga una configuración para editarla o reconstruir una simulación. */
    public void mostrarConfiguracion(Configuracion configuracion) {
        if (configuracion == null) throw new IllegalArgumentException("La configuracion es obligatoria");
        campoRam.setText(Integer.toString(configuracion.getRam()[0]));
        campoCiclo.setText(Integer.toString(configuracion.getDuracionMs()));
        campoQuantum.setText(Integer.toString(configuracion.getQuantum()));
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        setLayout(new java.awt.BorderLayout(0, 20));
        setBackground(new java.awt.Color(246, 248, 252));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 28, 24, 28));
        introduccion = new javax.swing.JLabel("<html><span style='font-size:22px'>Prepara tu simulacion</span><br><br>Define la memoria y la velocidad.<br>Crea procesos o carga el ejemplo para comenzar.</html>");
        introduccion.setForeground(new java.awt.Color(30, 49, 77));
        add(introduccion, java.awt.BorderLayout.NORTH);
        formulario = new javax.swing.JPanel(new java.awt.GridBagLayout());
        formulario.setOpaque(false);
        etiquetaCantidad = new javax.swing.JLabel("<html><b>Computadores</b><br><span style='color:#637089'>Proyecto 2 comienza con un unico equipo</span></html>");
        campoCantidad = new javax.swing.JTextField("1", 12);
        campoCantidad.setEditable(false);
        etiquetaRam = new javax.swing.JLabel("<html><b>Memoria del computador</b><br><span style='color:#637089'>Unidades de RAM disponibles</span></html>");
        campoRam = new javax.swing.JTextField("128", 12);
        etiquetaCiclo = new javax.swing.JLabel("<html><b>Velocidad de simulacion</b><br><span style='color:#637089'>Milisegundos por ciclo<br>Mayor valor = mas lento</span></html>");
        campoCiclo = new javax.swing.JTextField("700", 12);
        etiquetaPolitica = new javax.swing.JLabel("Orden de ejecucion");
        valorPolitica = new javax.swing.JLabel("<html><b>FCFS · politica inicial</b><br>Primero en llegar, primero en ejecutar<br><span style='color:#637089'>Round Robin, EDF y Prioridades:<br>disponibles en Planificacion E/S y metricas</span></html>");
        opciones = new javax.swing.JCheckBox("Mostrar parametros de integracion"); opciones.setOpaque(false);
        avanzados = new javax.swing.JPanel(new java.awt.GridBagLayout()); avanzados.setOpaque(false);
        etiquetaQuantum = new javax.swing.JLabel("Quantum inicial");
        campoQuantum = new javax.swing.JTextField("4", 12);
        aviso = new javax.swing.JLabel("<html>El quantum se usa con Round Robin.<br>Aplicar crea una sesion nueva.<br>Deten el reloj antes de cambiar la configuracion.</html>");
        aviso.setForeground(new java.awt.Color(93, 108, 130));
        java.awt.GridBagConstraints c;
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=0; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,20,24); formulario.add(etiquetaCantidad,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=0; c.anchor=java.awt.GridBagConstraints.WEST; c.weightx=1; c.fill=java.awt.GridBagConstraints.HORIZONTAL; c.insets=new java.awt.Insets(0,0,20,24); formulario.add(campoCantidad,c);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=1; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,20,24); formulario.add(etiquetaRam,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=1; c.anchor=java.awt.GridBagConstraints.WEST; c.weightx=1; c.fill=java.awt.GridBagConstraints.HORIZONTAL; c.insets=new java.awt.Insets(0,0,20,24); formulario.add(campoRam,c);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=2; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,20,24); formulario.add(etiquetaCiclo,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=2; c.anchor=java.awt.GridBagConstraints.WEST; c.weightx=1; c.fill=java.awt.GridBagConstraints.HORIZONTAL; c.insets=new java.awt.Insets(0,0,20,24); formulario.add(campoCiclo,c);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=3; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,20,24); formulario.add(etiquetaPolitica,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=3; c.anchor=java.awt.GridBagConstraints.WEST; c.weightx=1; c.fill=java.awt.GridBagConstraints.HORIZONTAL; c.insets=new java.awt.Insets(0,0,20,24); formulario.add(valorPolitica,c);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=4; c.gridwidth=2; c.anchor=java.awt.GridBagConstraints.WEST; c.insets=new java.awt.Insets(0,0,14,0); formulario.add(opciones,c);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=5; c.gridwidth=2; c.anchor=java.awt.GridBagConstraints.WEST; formulario.add(avanzados,c);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=0; c.insets=new java.awt.Insets(0,0,12,24); c.anchor=java.awt.GridBagConstraints.WEST; avanzados.add(etiquetaQuantum,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=0; c.insets=new java.awt.Insets(0,0,12,0); avanzados.add(campoQuantum,c);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=6; c.gridwidth=2; c.anchor=java.awt.GridBagConstraints.WEST; formulario.add(aviso,c);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=7; c.weighty=1; formulario.add(javax.swing.Box.createVerticalGlue(),c);
        add(formulario, java.awt.BorderLayout.CENTER);
        campoCantidad.setToolTipText("Proyecto 2 utiliza un computador"); campoCantidad.setName("cantidad");
        campoRam.setToolTipText("Ejemplo: 128"); campoRam.setName("ram");
        campoCiclo.setToolTipText("Entre 1 y 60000 ms. Recomendado: 700 para observar procesos"); campoCiclo.setName("ciclo");
        campoQuantum.setName("quantum");
        etiquetaCantidad.setLabelFor(campoCantidad); etiquetaRam.setLabelFor(campoRam); etiquetaCiclo.setLabelFor(campoCiclo);
        for (javax.swing.JTextField campo : new javax.swing.JTextField[] {campoCantidad, campoRam, campoCiclo, campoQuantum}) {
            campo.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
            campo.setMargin(new java.awt.Insets(6, 8, 6, 8));
        }
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel formulario, avanzados;
    private javax.swing.JCheckBox opciones;
    private javax.swing.JLabel introduccion, aviso;
    private javax.swing.JTextField campoCantidad, campoRam, campoCiclo, campoQuantum;
    private javax.swing.JLabel etiquetaCantidad, etiquetaRam, etiquetaCiclo, etiquetaQuantum, etiquetaPolitica, valorPolitica;
    // End of variables declaration//GEN-END:variables
}
