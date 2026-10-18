package Interfaces;

import Modelo.Configuracion;

/**
 * Entrada de parámetros iniciales del clúster.
 * @author Adolfo
 */
public class PanelConfiguracion extends javax.swing.JPanel {
    public PanelConfiguracion() { initComponents(); avanzados.setVisible(false); opciones.addActionListener(evento -> { avanzados.setVisible(opciones.isSelected()); revalidate(); }); }

    /** Lee los campos y valida su formato antes de construir la configuración. */
    public Configuracion leerConfiguracion() {
        for (javax.swing.JTextField campo : new javax.swing.JTextField[] {campoCantidad, campoRam, campoCiclo, campoQuantum, campoLatencia})
            campo.setBorder(javax.swing.UIManager.getBorder("TextField.border"));
        int cantidad = leerEntero(campoCantidad, "Computadores", 2, Integer.MAX_VALUE);
        String[] valores = campoRam.getText().split(",", -1);
        if (cantidad != valores.length) throw rechazar(campoRam, "Indica una RAM por computador. Para dos equipos: 64,96");
        int[] ram = new int[valores.length];
        for (int i = 0; i < ram.length; i++) {
            try { ram[i] = Integer.parseInt(valores[i].trim()); }
            catch (NumberFormatException error) { throw rechazar(campoRam, "La RAM del computador " + (i + 1) + " debe ser un entero positivo"); }
            if (ram[i] < 1) throw rechazar(campoRam, "La RAM del computador " + (i + 1) + " debe ser positiva");
        }
        return new Configuracion(ram, leerEntero(campoCiclo, "Duracion del ciclo en ms", 1, 60000),
                leerEntero(campoQuantum, "Quantum", 1, Integer.MAX_VALUE), leerEntero(campoLatencia, "Latencia", 0, Integer.MAX_VALUE));
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
        if (campo == campoQuantum || campo == campoLatencia) { opciones.setSelected(true); avanzados.setVisible(true); revalidate(); }
        campo.requestFocusInWindow(); campo.selectAll();
        return new IllegalArgumentException(mensaje);
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
        setLayout(new java.awt.BorderLayout(0, 20));
        setBackground(new java.awt.Color(246, 248, 252));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 28, 24, 28));
        introduccion = new javax.swing.JLabel("<html><span style='font-size:22px'>Prepara tu simulacion</span><br><br>Define la memoria y la velocidad.<br>Crea procesos o carga el ejemplo para comenzar.</html>");
        introduccion.setForeground(new java.awt.Color(30, 49, 77));
        add(introduccion, java.awt.BorderLayout.NORTH);
        formulario = new javax.swing.JPanel(new java.awt.GridBagLayout());
        formulario.setOpaque(false);
        etiquetaCantidad = new javax.swing.JLabel("<html><b>Computadores</b><br><span style='color:#637089'>Al menos dos equipos independientes</span></html>");
        campoCantidad = new javax.swing.JTextField("2", 12);
        etiquetaRam = new javax.swing.JLabel("<html><b>Memoria por computador</b><br><span style='color:#637089'>Ejemplo para dos equipos: 64,96</span></html>");
        campoRam = new javax.swing.JTextField("128,128", 12);
        etiquetaCiclo = new javax.swing.JLabel("<html><b>Velocidad de simulacion</b><br><span style='color:#637089'>Milisegundos por ciclo<br>Mayor valor = mas lento</span></html>");
        campoCiclo = new javax.swing.JTextField("700", 12);
        etiquetaPolitica = new javax.swing.JLabel("Orden de ejecucion");
        valorPolitica = new javax.swing.JLabel("<html><b>FCFS · disponible</b><br>Primero en llegar, primero en ejecutar<br><span style='color:#637089'>Round Robin, EDF y Prioridades:<br>pendientes de implementacion</span></html>");
        opciones = new javax.swing.JCheckBox("Mostrar parametros de integracion"); opciones.setOpaque(false);
        avanzados = new javax.swing.JPanel(new java.awt.GridBagLayout()); avanzados.setOpaque(false);
        etiquetaQuantum = new javax.swing.JLabel("Quantum inicial");
        campoQuantum = new javax.swing.JTextField("4", 12);
        etiquetaLatencia = new javax.swing.JLabel("Latencia de red (ciclos)");
        campoLatencia = new javax.swing.JTextField("0", 12);
        aviso = new javax.swing.JLabel("<html>Quantum y latencia se guardan para los modulos pendientes.<br>Aplicar crea una sesion nueva.<br>Deten el reloj antes de cambiar la configuracion.</html>");
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
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=1; c.insets=new java.awt.Insets(0,0,12,24); c.anchor=java.awt.GridBagConstraints.WEST; avanzados.add(etiquetaLatencia,c);
        c = new java.awt.GridBagConstraints(); c.gridx=1; c.gridy=1; c.insets=new java.awt.Insets(0,0,12,0); avanzados.add(campoLatencia,c);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=6; c.gridwidth=2; c.anchor=java.awt.GridBagConstraints.WEST; formulario.add(aviso,c);
        c = new java.awt.GridBagConstraints(); c.gridx=0; c.gridy=7; c.weighty=1; formulario.add(javax.swing.Box.createVerticalGlue(),c);
        add(formulario, java.awt.BorderLayout.CENTER);
        campoCantidad.setToolTipText("Ejemplo: 2"); campoCantidad.setName("cantidad");
        campoRam.setToolTipText("Ejemplo: 64,96 para dos computadores"); campoRam.setName("ram");
        campoCiclo.setToolTipText("Entre 1 y 60000 ms. Recomendado: 700 para observar procesos"); campoCiclo.setName("ciclo");
        campoQuantum.setName("quantum"); campoLatencia.setName("latencia");
        etiquetaCantidad.setLabelFor(campoCantidad); etiquetaRam.setLabelFor(campoRam); etiquetaCiclo.setLabelFor(campoCiclo);
        for (javax.swing.JTextField campo : new javax.swing.JTextField[] {campoCantidad, campoRam, campoCiclo, campoQuantum, campoLatencia}) {
            campo.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
            campo.setMargin(new java.awt.Insets(6, 8, 6, 8));
        }
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel formulario, avanzados;
    private javax.swing.JCheckBox opciones;
    private javax.swing.JLabel introduccion, aviso;
    private javax.swing.JTextField campoCantidad, campoRam, campoCiclo, campoQuantum, campoLatencia;
    private javax.swing.JLabel etiquetaCantidad, etiquetaRam, etiquetaCiclo, etiquetaQuantum, etiquetaLatencia, etiquetaPolitica, valorPolitica;
    // End of variables declaration//GEN-END:variables
}