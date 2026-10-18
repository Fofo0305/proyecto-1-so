package Interfaces;

import Modelo.VistaComputador;
import Modelo.VistaProceso;
import java.awt.*;
import javax.swing.*;

/** Representacion grafica de un computador, sin ejecutar la simulacion. @author Adolfo */
public class PanelComputador extends JPanel {
    private static final Color FONDO = new Color(242, 246, 251);
    private static final Color AZUL = new Color(42, 99, 195);

    public PanelComputador() {
        initComponents();
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(212, 222, 235)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
    }

    public void mostrar(VistaComputador vista) {
        removeAll();
        JPanel resumen = new JPanel(new BorderLayout(12, 8));
        resumen.setOpaque(false);
        JLabel nombre = new JLabel("Computador " + vista.id() + "   /   FCFS   /   ciclo " + vista.ciclo());
        nombre.setFont(new Font("SansSerif", Font.BOLD, 18));
        nombre.setForeground(new Color(26, 46, 75));
        resumen.add(nombre, BorderLayout.WEST);
        int capacidad = vista.usada() + vista.libre();
        JProgressBar ram = new JProgressBar(0, capacidad);
        ram.setValue(vista.usada()); ram.setStringPainted(true);
        ram.setString("RAM  " + vista.usada() + " / " + capacidad + "  ·  libre " + vista.libre());
        ram.setForeground(vista.libre() == 0 ? new Color(214, 137, 39) : AZUL);
        ram.setPreferredSize(new Dimension(300, 26));
        resumen.add(ram, BorderLayout.EAST);
        JPanel cuerpo = new JPanel(new BorderLayout(12, 12)); cuerpo.setOpaque(false);
        JPanel cpu = new JPanel(new BorderLayout(10, 0));
        cpu.setBackground(new Color(229, 239, 255));
        cpu.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel tituloCPU = new JLabel("CPU  ·  " + vista.modo());
        tituloCPU.setFont(new Font("SansSerif", Font.BOLD, 15)); tituloCPU.setForeground(AZUL);
        cpu.add(tituloCPU, BorderLayout.WEST);
        if (vista.procesoCPU() == null) cpu.add(new JLabel("Libre · esperando un proceso listo"), BorderLayout.CENTER);
        else cpu.add(tarjeta(vista.procesoCPU(), AZUL), BorderLayout.CENTER);
        cuerpo.add(cpu, BorderLayout.NORTH);
        JPanel colas = new JPanel(new GridLayout(1, 4, 12, 0)); colas.setOpaque(false);
        colas.add(cola("NUEVOS", "Esperan RAM", vista.colaNuevos(), new Color(174, 112, 24)));
        colas.add(cola("LISTOS", "FIFO · el primero pasa a CPU", vista.colaListos(), AZUL));
        colas.add(cola("BLOQUEADOS", "Esperan un recurso", vista.colaBloqueados(), new Color(135, 80, 171)));
        colas.add(cola("TERMINADOS", "Salieron de ejecucion", vista.colaTerminados(), new Color(29, 126, 101)));
        cuerpo.add(colas, BorderLayout.CENTER);
        add(resumen, BorderLayout.NORTH); add(cuerpo, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    private JPanel cola(String titulo, String ayuda, VistaProceso[] procesos, Color color) {
        JPanel columna = new JPanel(new BorderLayout(0, 8)); columna.setBackground(FONDO);
        columna.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel cabecera = new JPanel(new GridLayout(2, 1, 0, 4)); cabecera.setOpaque(false);
        JLabel etiqueta = new JLabel(titulo + "   " + procesos.length);
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 13)); etiqueta.setForeground(color);
        cabecera.add(etiqueta); JLabel pista = new JLabel(ayuda); pista.setFont(new Font("SansSerif", Font.PLAIN, 11)); cabecera.add(pista);
        columna.add(cabecera, BorderLayout.NORTH);
        JPanel tarjetas = new JPanel(); tarjetas.setLayout(new BoxLayout(tarjetas, BoxLayout.Y_AXIS)); tarjetas.setOpaque(false);
        if (procesos.length == 0) {
            JLabel vacia = new JLabel("Sin procesos"); vacia.setForeground(new Color(113, 127, 147)); tarjetas.add(vacia);
        }
        for (int i = 0; i < procesos.length; i++) {
            JLabel orden = new JLabel((i + 1) + (i == 0 ? " · Frente de cola" : " · Siguiente"));
            orden.setAlignmentX(LEFT_ALIGNMENT);
            orden.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            orden.setFont(new Font("SansSerif", Font.PLAIN, 10)); orden.setForeground(color);
            tarjetas.add(orden); tarjetas.add(Box.createVerticalStrut(3));
            tarjetas.add(tarjeta(procesos[i], color)); tarjetas.add(Box.createVerticalStrut(8));
        }
        JScrollPane desplazamiento = new JScrollPane(tarjetas);
        desplazamiento.setBorder(null); desplazamiento.getViewport().setBackground(FONDO);
        desplazamiento.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(16);
        columna.add(desplazamiento, BorderLayout.CENTER);
        return columna;
    }

    private JPanel tarjeta(VistaProceso proceso, Color color) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 5)); tarjeta.setBackground(Color.WHITE);
        tarjeta.setAlignmentX(LEFT_ALIGNMENT);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                BorderFactory.createEmptyBorder(8, 9, 8, 9)));
        JLabel nombre = new JLabel("P" + proceso.id() + " · " + proceso.nombre());
        nombre.setFont(new Font("SansSerif", Font.BOLD, 12)); nombre.setToolTipText(proceso.nombre());
        tarjeta.add(nombre, BorderLayout.NORTH);
        JLabel pcb = new JLabel("<html>RAM " + proceso.memoria() + " · prioridad " + proceso.prioridad()
                + "<br>PC " + proceso.pc() + " · MAR " + proceso.mar()
                + "<br>Restan " + proceso.restante() + " · plazo " + proceso.deadline() + "</html>");
        pcb.setFont(new Font("SansSerif", Font.PLAIN, 11)); tarjeta.add(pcb, BorderLayout.CENTER);
        long total = (long) proceso.pc() + proceso.restante();
        JProgressBar progreso = new JProgressBar(0, 100);
        progreso.setValue(total == 0 ? 100 : (int) (100L * proceso.pc() / total));
        progreso.setForeground(color); progreso.setStringPainted(true); progreso.setString(progreso.getValue() + "% ejecutado");
        tarjeta.add(progreso, BorderLayout.SOUTH);
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        return tarjeta;
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setPreferredSize(new Dimension(1080, 420));
    }// </editor-fold>//GEN-END:initComponents
}
