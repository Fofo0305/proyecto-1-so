package Pruebas;

import Modelo.CargaInicial;
import Modelo.Configuracion;
import Modelo.DefinicionProceso;
import Modelo.EstadoProceso;
import Modelo.TipoProceso;
import Modelo.VistaComputador;
import Interfaces.PanelConfiguracion;
import Sistema.ControladorSimulacion;

/** Contrato mínimo que debe conservar la reestructuración hacia Proyecto 2. @author Adolfo */
public final class PruebaBaseProyecto2 {
    private PruebaBaseProyecto2() { }

    public static void main(String[] args) {
        try {
            new Configuracion(new int[0], 300, 4);
            throw new AssertionError("Se acepto una simulacion sin computadores");
        } catch (IllegalArgumentException esperado) { }
        try {
            new Configuracion(new int[] {64, 64}, 300, 4);
            throw new AssertionError("Se acepto mas de un computador");
        } catch (IllegalArgumentException esperado) { }
        if (TipoProceso.values().length != 2) {
            throw new AssertionError("Persisten tipos productor o consumidor");
        }
        try {
            new DefinicionProceso("Equipo inexistente", 2, 0, 8, 1, 10);
            throw new AssertionError("Se acepto un proceso dirigido a un segundo computador");
        } catch (IllegalArgumentException esperado) { }

        Configuracion configuracion = new Configuracion(new int[] {64}, 300, 4);
        if (configuracion.getCantidadComputadores() != 1 || configuracion.getRam()[0] != 64) {
            throw new AssertionError("La configuracion no representa un unico computador");
        }
        Configuracion formulario = new PanelConfiguracion().leerConfiguracion();
        if (formulario.getCantidadComputadores() != 1 || formulario.getRam()[0] != 128) {
            throw new AssertionError("El formulario no propone la base de un solo computador");
        }

        CargaInicial carga = new CargaInicial(configuracion,
                new DefinicionProceso[] {new DefinicionProceso("Base P2", 1, 0, 16, 2, 20)});

        try (ControladorSimulacion controlador = new ControladorSimulacion()) {
            controlador.cargarCarga(carga);
            VistaComputador[] inicial = controlador.consultarVistas();
            if (inicial.length != 1 || inicial[0].usada() != 16 || inicial[0].colaListos().length != 1) {
                throw new AssertionError("El proceso no fue admitido en el computador unico");
            }
            try {
                controlador.crearProceso("Equipo inexistente", 2, 0, 8, 1, 10);
                throw new AssertionError("El controlador acepto un proceso para el equipo 2");
            } catch (IllegalArgumentException esperado) { }
            VistaComputador despuesDelRechazo = controlador.consultarVistas()[0];
            if (despuesDelRechazo.usada() != 16 || despuesDelRechazo.colaListos().length != 1) {
                throw new AssertionError("El rechazo modifico la sesion cargada");
            }

            controlador.avanzarCiclo();
            VistaComputador vista = controlador.consultarVistas()[0];
            if (vista.ciclo() != 1 || vista.procesoCPU() == null
                    || vista.procesoCPU().estado() != EstadoProceso.EJECUCION
                    || vista.procesoCPU().restante() != 1) {
                throw new AssertionError("El computador unico no avanzo exactamente un ciclo");
            }
        }

        System.out.println("OK: configuracion, admision y reloj con un solo computador");
    }
}
