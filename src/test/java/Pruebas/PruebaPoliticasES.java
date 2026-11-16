package Pruebas;

import Modelo.*;
import Persistencia.ArchivoConfiguracion;
import Sistema.ControladorSimulacion;
import java.nio.file.Files;
import java.nio.file.Path;

/** Regresión de políticas, E/S temporal, métricas y persistencia transitoria. */
public final class PruebaPoliticasES {
    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static ControladorSimulacion crear() {
        ControladorSimulacion controlador = new ControladorSimulacion();
        controlador.configurar(new Configuracion(new int[] {64}, 1, 2, 0));
        return controlador;
    }

    public static void main(String[] args) throws Exception {
        for (PoliticaPlanificacion politica : PoliticaPlanificacion.values()) {
            try (ControladorSimulacion controlador = crear()) {
                controlador.cambiarPolitica(1, politica, 2);
                controlador.crearProceso("A", 1, 2, 4, 5, 100);
                controlador.crearProceso("B", 1, 1, 4, 5, 100);
                for (int ciclo = 0; ciclo < 20; ciclo++) controlador.avanzarCiclo();
                VistaMetricas metrica = controlador.consultarMetricas()[0];
                verificar(metrica.completados() == 2 && metrica.vencidos() == 0,
                        "La politica no completa la carga: " + politica);
            }
        }

        try (ControladorSimulacion controlador = crear()) {
            Proceso io = controlador.crearProcesoES("ES", 1, 1, 4, 6, 100, 2, 3);
            controlador.avanzarCiclo();
            controlador.avanzarCiclo();
            verificar(io.getEstado() == EstadoProceso.BLOQUEADO && io.getPc() == 2, "No bloquea al agotar rafaga");
            controlador.avanzarCiclo(); controlador.avanzarCiclo();
            verificar(io.getEstado() == EstadoProceso.BLOQUEADO, "E/S demasiado corta");
            controlador.avanzarCiclo();
            verificar(io.getEstado() == EstadoProceso.LISTO, "No retorna al cumplir E/S");

            Path archivo = Path.of("target/pruebas-buffers/io-roundtrip.csv");
            controlador.cambiarPolitica(1, PoliticaPlanificacion.EDF, 5);
            ArchivoConfiguracion.guardar(archivo, controlador.getCargaInicial());
            controlador.cargarCarga(ArchivoConfiguracion.cargar(archivo));
            verificar(controlador.getComputadores()[0].getPolitica() == PoliticaPlanificacion.EDF,
                    "Pierde politica CSV");
            verificar(controlador.getCargaInicial().getProcesos()[0].parametrosES().equals(new ParametrosES(2, 3)),
                    "Pierde parametros E/S");

            Path duplicado = Path.of("target/pruebas-proyecto2/config-duplicado.csv");
            Files.writeString(duplicado, "AVILAOS,3\nCONFIG,1,2,0\nCONFIG,1,2,0\n"
                    + "COMPUTADOR,1,64,FCFS,2\n");
            try {
                controlador.cargarCarga(ArchivoConfiguracion.cargar(duplicado));
                throw new AssertionError("Acepta dos registros CONFIG");
            } catch (IllegalArgumentException esperado) { }
            verificar(controlador.getComputadores()[0].getPolitica() == PoliticaPlanificacion.EDF,
                    "Un CSV invalido reemplazo la sesion activa");

            Path computadores = Path.of("target/pruebas-proyecto2/computador-duplicado.csv");
            Files.writeString(computadores, "AVILAOS,3\nCONFIG,1,2,0\n"
                    + "COMPUTADOR,1,0,FCFS,2\nCOMPUTADOR,1,64,FCFS,2\n");
            try {
                ArchivoConfiguracion.cargar(computadores);
                throw new AssertionError("Acepta dos registros COMPUTADOR si el primero tiene RAM cero");
            } catch (IllegalArgumentException esperado) { }
        }
        System.out.println("OK: cuatro politicas, E/S, metricas y CSV transitorio en un computador");
    }
}
