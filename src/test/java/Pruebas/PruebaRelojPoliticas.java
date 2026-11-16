package Pruebas;

import Modelo.Configuracion;
import Modelo.PoliticaPlanificacion;
import Sistema.ControladorSimulacion;

/** Reloj real, cambios de política, pausa y cierre del computador único. */
public final class PruebaRelojPoliticas {
    public static void main(String[] args) throws Exception {
        try (ControladorSimulacion controlador = new ControladorSimulacion()) {
            controlador.configurar(new Configuracion(new int[] {64}, 2, 2));
            controlador.crearProcesoES("IO", 1, 2, 4, 30, 1000, 2, 2);
            controlador.iniciar();
            for (int i = 0; i < 40; i++) {
                controlador.cambiarPolitica(1, PoliticaPlanificacion.values()[i % 4], 1 + i % 3);
                controlador.consultarMetricas();
                controlador.consultarLog();
                Thread.sleep(2);
            }
            controlador.pausar();
            long ciclo = controlador.getCiclo();
            Thread.sleep(20);
            if (controlador.getCiclo() != ciclo) throw new AssertionError("Pausa no estable");
            controlador.reanudar();
            Thread.sleep(30);
            controlador.detener();
            if (!"Detenido".equals(controlador.getEstado())) throw new AssertionError("Reloj no se detuvo");
        }
        System.out.println("OK: reloj, cambios de politica, consultas, pausa y cierre");
    }
}
