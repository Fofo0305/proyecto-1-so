package Interfaces;

/** Ajusta las tarjetas al ancho del viewport y permite recorrer el cluster verticalmente. @author Adolfo */
public class ContenidoComputadores extends javax.swing.JPanel implements javax.swing.Scrollable {
    public ContenidoComputadores() { initComponents(); }
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() { setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); }
    // </editor-fold>//GEN-END:initComponents
    @Override public java.awt.Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientacion, int direccion) { return 24; }
    @Override public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientacion, int direccion) { return Math.max(24, visible.height - 24); }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
