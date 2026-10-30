package Pruebas;

/**
 * Comprueba que la ventana de 900x680 no exige maximizarse: los controles del reloj caben
 * en el area util, el formulario de procesos conserva sus valores y admite procesos, y el
 * tablero de computadores no obliga a desplazarse en horizontal.
 */
public final class PruebaVentanaNormal {

    public static void main(String[] args) throws Exception {
        Interfaces.VentanaPrincipal[] ventanaNormal = new Interfaces.VentanaPrincipal[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ventanaNormal[0] = new Interfaces.VentanaPrincipal();
                ventanaNormal[0].setSize(900, 680); ventanaNormal[0].setVisible(true); ventanaNormal[0].validate();
                if ((ventanaNormal[0].getExtendedState() & java.awt.Frame.MAXIMIZED_BOTH) != 0)
                    throw new AssertionError("La ventana requiere maximizarse");
                java.awt.Rectangle areaNormal = new java.awt.Rectangle(ventanaNormal[0].getContentPane().getSize());
                Interfaces.PanelControles controlesNormal = ventanaNormal[0].getPanelControles();
                javax.swing.JPanel accionesNormal = (javax.swing.JPanel) controlesNormal.getComponent(0);
                for (java.awt.Component botonNormal : accionesNormal.getComponents()) {
                    java.awt.Rectangle limitesNormal = javax.swing.SwingUtilities.convertRectangle(botonNormal.getParent(), botonNormal.getBounds(), ventanaNormal[0].getContentPane());
                    if (!areaNormal.contains(limitesNormal)) throw new AssertionError("Control fuera de ventana: " + limitesNormal);
                }
                ventanaNormal[0].mostrarProcesos(); ventanaNormal[0].validate();
                Interfaces.PanelProceso formularioNormal = ventanaNormal[0].getPanelProceso();
                String[] camposNormal = formularioNormal.leerCampos();
                if (!"Proceso".equals(camposNormal[0]) || !"32".equals(camposNormal[3])) throw new AssertionError("Formulario cambia valores");
                java.awt.image.BufferedImage imagenNormal = new java.awt.image.BufferedImage(900, 680, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D pincelNormal = imagenNormal.createGraphics();
                try { ventanaNormal[0].printAll(pincelNormal); } finally { pincelNormal.dispose(); }
                try { javax.imageio.ImageIO.write(imagenNormal, "png", new java.io.File("target/procesos-ventana-normal.png")); }
                catch (java.io.IOException error) { throw new IllegalStateException(error); }
            });
            ventanaNormal[0].getControlador().configurar(new Modelo.Configuracion(new int[] {64, 96}, 700, 4, 0));
            String[][] solicitudNormal = new String[1][];
            javax.swing.SwingUtilities.invokeAndWait(() -> solicitudNormal[0] = ventanaNormal[0].getPanelProceso().leerCampos());
            Modelo.Proceso procesoNormal = Interfaces.PanelProceso.crearProceso(ventanaNormal[0].getControlador(), solicitudNormal[0]);
            if (procesoNormal.getEstado() != Modelo.EstadoProceso.LISTO) throw new AssertionError("Nuevo formulario no admite proceso");
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                Interfaces.PanelComputadores tableroNormal = ventanaNormal[0].getPanelComputadores();
                tableroNormal.mostrarVistas(ventanaNormal[0].getControlador().consultarVistas(), "Preparado", 0);
                java.awt.Container contenidoNormal = tableroNormal.getParent();
                ((javax.swing.JTabbedPane) contenidoNormal).setSelectedComponent(tableroNormal);
                ventanaNormal[0].validate();
                javax.swing.JScrollPane scrollNormal = null;
                for (java.awt.Component componenteNormal : tableroNormal.getComponents())
                    if (componenteNormal instanceof javax.swing.JScrollPane encontradoNormal) scrollNormal = encontradoNormal;
                if (scrollNormal == null || scrollNormal.getViewport().getView().getWidth() > scrollNormal.getViewport().getWidth())
                    throw new AssertionError("El tablero exige desplazamiento horizontal en ventana normal");
            });
            System.out.println("Ventana 900x680 sin maximizar: cuatro controles visibles, formulario consistente y creacion/admision desde Procesos: OK");
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
        catch (java.lang.reflect.InvocationTargetException error) { throw new IllegalStateException(error.getCause()); }
        finally {
            if (ventanaNormal[0] != null) {
                ventanaNormal[0].getControlador().close();
                javax.swing.SwingUtilities.invokeLater(() -> ventanaNormal[0].dispose());
            }
        }
    }
}
