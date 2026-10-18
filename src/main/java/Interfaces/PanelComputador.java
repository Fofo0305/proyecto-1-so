package Interfaces;

import Modelo.VistaComputador;
import Modelo.VistaProceso;
import java.awt.*;
import javax.swing.*;

/** Representacion grafica de un computador, sin ejecutar la simulacion. @author Adolfo */
public class PanelComputador extends JPanel {
    private static final Color FONDO = new Color(242, 246, 251);
    private static final Color AZUL = new Color(42, 99, 195);
    private boolean mostrarPCB;
    private final JLabel nombre = new JLabel();
    private final JProgressBar ram = new JProgressBar();
    private final JPanel cpu = new JPanel(new BorderLayout(10, 0));
    private final JLabel[] titulos = new JLabel[4];
    private final JPanel[] tarjetasCola = new JPanel[4];
    private final VistaProceso[][] procesosAnteriores = new VistaProceso[4][];
    private final boolean[] detallesAnteriores = new boolean[4];
    private final String[] nombresCola = {"NUEVOS", "LISTOS", "BLOQUEADOS", "TERMINADOS"};
    private final Color[] colores = {new Color(174, 112, 24), AZUL, new Color(135, 80, 171), new Color(29, 126, 101)};

    public void mostrarDetalles(boolean valor) { mostrarPCB = valor; }

    public PanelComputador() {
        initComponents();
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(212, 222, 235)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        JPanel resumen = new JPanel(new BorderLayout(12, 8)); resumen.setOpaque(false);
        nombre.setFont(new Font("SansSerif", Font.BOLD, 18)); nombre.setForeground(new Color(26, 46, 75));
        resumen.add(nombre, BorderLayout.WEST);
        ram.setStringPainted(true); ram.setPreferredSize(new Dimension(300, 26));
        resumen.add(ram, BorderLayout.EAST);
        JPanel cuerpo = new JPanel(new BorderLayout(12, 12)); cuerpo.setOpaque(false);
        cpu.setBackground(new Color(229, 239, 255));
        cpu.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        cuerpo.add(cpu, BorderLayout.NORTH);
        JPanel colas = new JPanel(new GridLayout(1, 4, 12, 0)); colas.setOpaque(false);
        String[] ayudas = {"Esperan RAM", "FIFO · el primero pasa a CPU", "Esperan un recurso", "Salieron de ejecucion"};
        for (int i = 0; i < 4; i++) colas.add(crearCola(i, ayudas[i]));
        cuerpo.add(colas, BorderLayout.CENTER);
        add(resumen, BorderLayout.NORTH); add(cuerpo, BorderLayout.CENTER);
    }

    /** Conserva los scrolls y sus modelos durante el refresco del PCB. */
    public void mostrar(VistaComputador vista) {
        nombre.setText("Computador " + vista.id() + "   /   FCFS   /   ciclo " + vista.ciclo());
        int capacidad = vista.usada() + vista.libre();
        ram.setMaximum(capacidad); ram.setValue(vista.usada());
        ram.setString("RAM  " + vista.usada() + " / " + capacidad + "  ·  libre " + vista.libre());
        ram.setForeground(vista.libre() == 0 ? new Color(214, 137, 39) : AZUL);
        cpu.removeAll();
        JLabel tituloCPU = new JLabel("CPU  ·  " + vista.modo());
        tituloCPU.setFont(new Font("SansSerif", Font.BOLD, 15)); tituloCPU.setForeground(AZUL);
        cpu.add(tituloCPU, BorderLayout.WEST);
        if (vista.procesoCPU() == null) cpu.add(new JLabel("Libre · esperando un proceso listo"), BorderLayout.CENTER);
        else cpu.add(tarjeta(vista.procesoCPU(), AZUL), BorderLayout.CENTER);
        actualizarCola(0, vista.colaNuevos()); actualizarCola(1, vista.colaListos());
        actualizarCola(2, vista.colaBloqueados()); actualizarCola(3, vista.colaTerminados());
        revalidate(); repaint();
    }

    private JPanel crearCola(int indice, String ayuda) {
        JPanel columna = new JPanel(new BorderLayout(0, 8)); columna.setBackground(FONDO);
        columna.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel cabecera = new JPanel(new GridLayout(2, 1, 0, 4)); cabecera.setOpaque(false);
        titulos[indice] = new JLabel(nombresCola[indice] + "   0");
        titulos[indice].setFont(new Font("SansSerif", Font.BOLD, 13)); titulos[indice].setForeground(colores[indice]);
        cabecera.add(titulos[indice]); JLabel pista = new JLabel(ayuda); pista.setFont(new Font("SansSerif", Font.PLAIN, 11)); cabecera.add(pista);
        columna.add(cabecera, BorderLayout.NORTH);
        JPanel tarjetas = new JPanel(); tarjetas.setLayout(new BoxLayout(tarjetas, BoxLayout.Y_AXIS)); tarjetas.setOpaque(false);
        tarjetasCola[indice] = tarjetas;
        JScrollPane desplazamiento = new JScrollPane(tarjetas);
        desplazamiento.setName("cola-" + nombresCola[indice]);
        desplazamiento.setBorder(null); desplazamiento.getViewport().setBackground(FONDO);
        desplazamiento.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(16);
        columna.add(desplazamiento, BorderLayout.CENTER);
        return columna;
    }

    private void actualizarCola(int indice, VistaProceso[] procesos) {
        if (java.util.Arrays.equals(procesosAnteriores[indice], procesos) && detallesAnteriores[indice] == mostrarPCB) return;
        procesosAnteriores[indice] = procesos;
        detallesAnteriores[indice] = mostrarPCB;
        titulos[indice].setText(nombresCola[indice] + "   " + procesos.length);
        JPanel tarjetas = tarjetasCola[indice];
        tarjetas.removeAll();
        if (procesos.length == 0) {
            JLabel vacia = new JLabel("Sin procesos"); vacia.setForeground(new Color(113, 127, 147)); tarjetas.add(vacia);
        }
        for (int i = 0; i < procesos.length; i++) {
            JLabel orden = new JLabel((i + 1) + (i == 0 ? " · Frente de cola" : " · Siguiente"));
            orden.setAlignmentX(LEFT_ALIGNMENT);
            orden.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            orden.setFont(new Font("SansSerif", Font.PLAIN, 10)); orden.setForeground(colores[indice]);
            tarjetas.add(orden); tarjetas.add(Box.createVerticalStrut(3));
            tarjetas.add(tarjeta(procesos[i], colores[indice])); tarjetas.add(Box.createVerticalStrut(8));
        }
        tarjetas.revalidate(); tarjetas.repaint();
    }
    private JPanel tarjeta(VistaProceso proceso, Color color) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 5)); tarjeta.setBackground(Color.WHITE);
        tarjeta.setAlignmentX(LEFT_ALIGNMENT);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                BorderFactory.createEmptyBorder(8, 9, 8, 9)));
        JLabel nombre = new JLabel("P" + proceso.id() + " · " + proceso.nombre());
        nombre.setFont(new Font("SansSerif", Font.BOLD, 12)); nombre.setToolTipText(proceso.nombre());
        tarjeta.add(nombre, BorderLayout.NORTH);
        String datosPCB = "PC " + proceso.pc() + " · MAR " + proceso.mar() + " · prioridad " + proceso.prioridad();
        JLabel pcb = new JLabel("<html>RAM " + proceso.memoria()
                + " · restan " + proceso.restante() + "<br>Plazo " + proceso.deadline()
                + (mostrarPCB ? "<br>" + datosPCB : "") + "</html>");
        tarjeta.setToolTipText(datosPCB + " · " + proceso.estado());
        pcb.setFont(new Font("SansSerif", Font.PLAIN, 11)); tarjeta.add(pcb, BorderLayout.CENTER);
        long total = (long) proceso.pc() + proceso.restante();
        JProgressBar progreso = new JProgressBar(0, 100);
        progreso.setValue(total == 0 ? 100 : (int) (100L * proceso.pc() / total));
        progreso.setForeground(color); progreso.setStringPainted(true); progreso.setString(progreso.getValue() + "% ejecutado");
        tarjeta.add(progreso, BorderLayout.SOUTH);
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, mostrarPCB ? 126 : 104));
        return tarjeta;
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setPreferredSize(new Dimension(1080, 420));
        setMinimumSize(new Dimension(0, 420));
    }// </editor-fold>//GEN-END:initComponents
}
