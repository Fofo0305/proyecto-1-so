package Pruebas;

import Interfaces.PanelEstadoBuffers;
import Modelo.*;
import Sistema.ControladorSimulacion;
import javax.swing.*;

/** Contrato visual, copias, lectura asincrona y posicion del historial. @author Luciano Rojas */
public final class PruebaPanelObservacion {
    public static void main(String[] args) throws Exception {
        VistaObservacionBuffers vista = ejemplo(100);
        VistaBuffer[] copia = vista.estados();
        copia[0] = null;
        if (vista.estados()[0] == null) throw new AssertionError("La vista expone su arreglo");
        SwingUtilities.invokeAndWait(() -> {
            try {
                PanelEstadoBuffers panel = new PanelEstadoBuffers();
                JFrame marco = new JFrame();
                marco.setContentPane(panel);
                marco.setSize(1000,650);
                marco.addNotify();
                marco.validate();
                try {
                    panel.mostrarVista(vista);
                    if (!panel.getTextoEstado().contains("Producciones: 7")
                            || !panel.getTextoEstado().contains("promedio=2.50")
                            || !panel.getTextoProcesos().contains("espera actual: SEMAFORO")
                            || !panel.getTextoEventos().contains("#100 | ciclo 100")
                            || !panel.getTextoEventos().contains("descartados: 4"))
                        throw new AssertionError("Faltan datos del modulo");
                    JTabbedPane tabs = (JTabbedPane) panel.getComponent(1);
                    tabs.setSelectedIndex(2);
                    marco.validate();
                    JScrollPane scroll = (JScrollPane) tabs.getComponentAt(2);
                    scroll.getViewport().setViewPosition(new java.awt.Point(0,300));
                    panel.mostrarVista(ejemplo(120));
                    if (scroll.getViewport().getViewPosition().y != 300)
                        throw new AssertionError("El refresco pierde la posicion del historial");
                    String antes = panel.getTextoEventos();
                    panel.mostrarVista(ejemplo(120));
                    if (!antes.equals(panel.getTextoEventos()) || scroll.getViewport().getViewPosition().y != 300)
                        throw new AssertionError("Refrescar altera historial o scroll");
                    tabs.setSelectedIndex(0);
                    marco.validate();
                    java.awt.image.BufferedImage imagen = new java.awt.image.BufferedImage(1000,650,java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g = imagen.createGraphics();
                    marco.getRootPane().printAll(g);
                    g.dispose();
                    javax.imageio.ImageIO.write(imagen,"png",new java.io.File("target/pruebas-buffers/observacion-estado.png"));
                    tabs.setSelectedIndex(2);
                    marco.validate();
                    g = imagen.createGraphics(); marco.getRootPane().printAll(g); g.dispose();
                    javax.imageio.ImageIO.write(imagen,"png",new java.io.File("target/pruebas-buffers/observacion-eventos.png"));
                } finally { marco.dispose(); }
            } catch (java.io.IOException error) { throw new RuntimeException(error); }
        });
        try (ControladorSimulacion controlador = new ControladorSimulacion()) {
            controlador.configurar(new Configuracion(new int[]{16,16},700,4,0));
            controlador.crearBuffer("lectura-asincrona",2,1);
            PanelEstadoBuffers[] panel = new PanelEstadoBuffers[1];
            SwingUtilities.invokeAndWait(() -> { panel[0]=new PanelEstadoBuffers(controlador); panel[0].actualizar(); });
            long limite = System.nanoTime()+3_000_000_000L;
            boolean[] terminado = {false};
            while (!terminado[0] && System.nanoTime()<limite) {
                SwingUtilities.invokeAndWait(() -> terminado[0]=panel[0].getTextoEventos().contains("lectura-asincrona"));
                Thread.sleep(10);
            }
            if (!terminado[0]) throw new AssertionError("Lectura asincrona no termina");
        }
        System.out.println("OK: observacion Swing, copias, datos visibles y scroll estable");
    }
    private static VistaObservacionBuffers ejemplo(int cantidad) {
        EventoBuffer[] eventos = new EventoBuffer[cantidad];
        for (int i=0;i<cantidad;i++) eventos[i]=new EventoBuffer(i+1,i+1,"compartido",1,42,TipoEventoBuffer.PRODUCCION,"Insercion real");
        return new VistaObservacionBuffers(
            new VistaBuffer[]{new VistaBuffer("compartido",1,8,3,"a, b, c",1,5,3,"ninguna","ninguna","P42")},
            new VistaContadoresBuffer[]{new VistaContadoresBuffer("compartido",1,8,3,7,4)},
            new VistaBloqueosBuffer[]{new VistaBloqueosBuffer("compartido",5,2,1,3,1,0)},
            new VistaBloqueoProceso[]{new VistaBloqueoProceso(42,"Consumidor remoto",2,"compartido",5,2,3,1,MotivoBloqueo.SEMAFORO)},
            new VistaEventosBuffer[]{new VistaEventosBuffer("compartido",4,eventos)});
    }
}
