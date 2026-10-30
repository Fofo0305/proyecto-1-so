package Pruebas;

import Interfaces.VentanaPrincipal;
import Modelo.Configuracion;
import Modelo.TipoProceso;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import static Pruebas.PruebaSemaforos.verificar;

/** Flujo de creacion desde Swing y ejecucion posterior. @author Luciano Rojas */
public final class PruebaVentanaProductorConsumidor {
    public static void main(String[] args) throws Exception {
        VentanaPrincipal[] ventana = new VentanaPrincipal[1];
        SwingUtilities.invokeAndWait(() -> ventana[0] = new VentanaPrincipal());
        VentanaPrincipal v = ventana[0];
        try {
            v.getControlador().configurar(new Configuracion(new int[] {32,32},700,4,0));
            SwingUtilities.invokeAndWait(v::crearProductorConsumidor);
            esperar(v);
            verificar(v.getControlador().getCargaInicial().getProcesos().length == 0,
                    "Creo un proceso con buffer inexistente");
            v.getControlador().crearBuffer("Datos",1,1);
            SwingUtilities.invokeAndWait(v::crearProductorConsumidor);
            esperar(v);
            SwingUtilities.invokeAndWait(() -> {
                seleccionarConsumidor(v.getPanelProductorConsumidor());
                v.crearProductorConsumidor();
            });
            esperar(v);
            var carga = v.getControlador().getCargaInicial();
            verificar(carga.getProcesos().length == 2 && carga.getProcesos()[0].tipo() == TipoProceso.PRODUCTOR
                    && carga.getProcesos()[1].tipo() == TipoProceso.CONSUMIDOR, "El formulario no creo ambos tipos");
            var equipo = v.getControlador().getComputadores()[0];
            for (int ciclo = 1; ciclo <= 100 && equipo.getTerminados().length < 2; ciclo++) equipo.avanzarCiclo(ciclo);
            verificar(equipo.getTerminados().length == 2 && v.getControlador().consultarBuffers()[0].cantidad() == 0,
                    "La carga creada desde Swing no completo");
            System.out.println("OK: solicitudes Swing, buffer inexistente y flujo completo productor consumidor");
        } finally {
            v.getControlador().close();
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    private static void seleccionarConsumidor(java.awt.Container contenedor) {
        for (java.awt.Component componente : contenedor.getComponents()) {
            if (componente instanceof JComboBox<?> selector) selector.setSelectedItem("CONSUMIDOR");
            if (componente instanceof java.awt.Container hijo) seleccionarConsumidor(hijo);
        }
    }

    private static void esperar(VentanaPrincipal ventana) throws Exception {
        long limite = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < limite) {
            boolean[] pendiente = new boolean[1];
            SwingUtilities.invokeAndWait(() -> pendiente[0] = ventana.getEstado().equals("Creando productor o consumidor..."));
            if (!pendiente[0]) return;
            Thread.sleep(10);
        }
        throw new AssertionError("La solicitud no termino en cinco segundos");
    }
}
