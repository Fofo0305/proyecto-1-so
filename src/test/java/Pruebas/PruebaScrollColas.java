package Pruebas;

/**
 * Comprueba que refrescar el PCB no interrumpe el arrastre de la barra de desplazamiento:
 * conserva la misma barra, la posicion y el estado de ajuste, y deja llegar al final de la cola.
 */
public final class PruebaScrollColas {

    public static void main(String[] args) throws Exception {
        Sistema.Computador equipoScroll = new Sistema.Computador(1, 64);
        for (int procesoScroll = 0; procesoScroll < 16; procesoScroll++)
            equipoScroll.recibirProceso(new Modelo.Proceso("Proceso " + procesoScroll, 1, Modelo.TipoProceso.CPU_BOUND, 0, 1, 8, 100));
        equipoScroll.admitirProcesos();
        Interfaces.PanelComputador[] panelScroll = new Interfaces.PanelComputador[1];
        javax.swing.JFrame[] ventanaScroll = new javax.swing.JFrame[1];
        javax.swing.JScrollPane[] barraScroll = new javax.swing.JScrollPane[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                panelScroll[0] = new Interfaces.PanelComputador(); panelScroll[0].mostrar(equipoScroll.consultarVista());
                ventanaScroll[0] = new javax.swing.JFrame(); ventanaScroll[0].setContentPane(panelScroll[0]);
                ventanaScroll[0].setSize(1100, 440); ventanaScroll[0].setVisible(true); ventanaScroll[0].validate();
                javax.swing.JPanel cuerpoScroll = (javax.swing.JPanel) ((java.awt.BorderLayout) panelScroll[0].getLayout()).getLayoutComponent(java.awt.BorderLayout.CENTER);
                javax.swing.JPanel colasScroll = (javax.swing.JPanel) ((java.awt.BorderLayout) cuerpoScroll.getLayout()).getLayoutComponent(java.awt.BorderLayout.CENTER);
                javax.swing.JPanel listosScroll = (javax.swing.JPanel) colasScroll.getComponent(1);
                barraScroll[0] = (javax.swing.JScrollPane) ((java.awt.BorderLayout) listosScroll.getLayout()).getLayoutComponent(java.awt.BorderLayout.CENTER);
                if (barraScroll[0].getVerticalScrollBar().getMaximum() - barraScroll[0].getVerticalScrollBar().getVisibleAmount() < 200)
                    throw new AssertionError("La cola de prueba no tiene recorrido suficiente");
                barraScroll[0].getVerticalScrollBar().setValue(160);
                barraScroll[0].getVerticalScrollBar().setValueIsAdjusting(true);
            });
            equipoScroll.avanzarCiclo(1);
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                panelScroll[0].mostrar(equipoScroll.consultarVista()); ventanaScroll[0].validate();
                if (barraScroll[0].getParent() == null || !javax.swing.SwingUtilities.isDescendingFrom(barraScroll[0], panelScroll[0])
                        || !barraScroll[0].getVerticalScrollBar().getValueIsAdjusting()
                        || barraScroll[0].getVerticalScrollBar().getValue() != 160)
                    throw new AssertionError("Refresco interrumpe el arrastre o cambia posicion");
                barraScroll[0].getVerticalScrollBar().setValue(240);
            });
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                if (barraScroll[0].getVerticalScrollBar().getValue() != 240)
                    throw new AssertionError("Una restauracion diferida piso el arrastre");
                barraScroll[0].getVerticalScrollBar().setValueIsAdjusting(false);
                barraScroll[0].getVerticalScrollBar().setValue(barraScroll[0].getVerticalScrollBar().getMaximum());
                if (barraScroll[0].getViewport().getViewPosition().y <= 240) throw new AssertionError("No llega al final de cola");
            });
            System.out.println("Scroll: misma barra y arrastre activo tras refrescar PCB, posicion 160/240 estable y final accesible: OK");
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
        catch (java.lang.reflect.InvocationTargetException error) { throw new IllegalStateException(error.getCause()); }
        finally { if (ventanaScroll[0] != null) javax.swing.SwingUtilities.invokeLater(() -> ventanaScroll[0].dispose()); }
    }
}
