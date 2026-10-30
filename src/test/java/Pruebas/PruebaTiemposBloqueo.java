package Pruebas;

import Modelo.*;
import Sistema.ControladorSimulacion;
import Persistencia.ArchivoConfiguracion;
import java.nio.file.Path;
import static Pruebas.PruebaSemaforos.verificar;

/** Esperas abiertas, canceladas y satisfechas en ciclos simulados. @author Luciano Rojas */
public final class PruebaTiemposBloqueo {
    public static void main(String[] args) throws Exception {
        comprobarSemaforo(false);
        comprobarSemaforo(true);
        comprobarRed(0);
        comprobarRed(3);
        comprobarTotalesRemotos();
        System.out.println("OK: tiempos de semaforo y red, esperas abiertas, promedios y estadisticas tras limpieza");
    }

    private static ControladorSimulacion crear(int latencia) {
        ControladorSimulacion c = new ControladorSimulacion();
        c.configurar(new Configuracion(new int[]{32,32},1,4,latencia)); c.crearBuffer("B",1,1);
        return c;
    }

    private static void comprobarSemaforo(boolean cancelar) {
        try (ControladorSimulacion c = crear(0)) {
            Proceso p = c.crearProcesoBuffer("C",1,0,2,6,TipoProceso.CONSUMIDOR,"B",1,1);
            c.avanzarCiclo(); c.avanzarCiclo();
            VistaBloqueosBuffer inicio = c.consultarBloqueosBuffers()[0];
            verificar(inicio.esperasSemaforo() == 1 && inicio.abiertasSemaforo() == 1
                    && inicio.ciclosSemaforo() == 0 && inicio.promedioSemaforo() == 0,
                    "El wait conto como un ciclo adicional de bloqueo");
            c.avanzarCiclo(); c.avanzarCiclo();
            VistaBloqueosBuffer abierta = c.consultarBloqueosBuffers()[0];
            verificar(abierta.ciclosSemaforo() == 2 && abierta.promedioSemaforo() == 2
                    && abierta.abiertasSemaforo() == 1, "No incluyo la espera aun abierta");
            VistaBloqueoProceso proceso = c.consultarBloqueosProcesos()[0];
            verificar(proceso.proceso() == p.getId() && proceso.ciclosSemaforo() == 2
                    && proceso.esperaActual() == MotivoBloqueo.SEMAFORO, "Falta el tiempo del proceso");
            for (int i = 0; i < 20; i++) {
                c.consultarBloqueosProcesos(); c.consultarBloqueosBuffers(); c.consultarVistas();
            }
            verificar(c.consultarBloqueosBuffers()[0].ciclosSemaforo() == 2, "Consultar sumo ciclos");
            if (cancelar) c.cancelarProceso(p);
            for (int i = 0; i < 5; i++) c.avanzarCiclo();
            VistaBloqueosBuffer fin = c.consultarBloqueosBuffers()[0];
            long esperado = cancelar ? 2 : 4;
            verificar(fin.ciclosSemaforo() == esperado && fin.abiertasSemaforo() == 0
                    && fin.esperasSemaforo() == 1 && fin.promedioSemaforo() == esperado,
                    "La limpieza borro o duplico el tiempo acumulado");
            verificar(c.consultarBloqueosProcesos()[0].ciclosSemaforo() == esperado
                    && c.consultarBloqueosProcesos()[0].esperaActual() == MotivoBloqueo.NINGUNO,
                    "El terminado perdio sus estadisticas");
            verificar(abierta.ciclosSemaforo() == 2 && abierta.abiertasSemaforo() == 1,
                    "Se altero la instantanea anterior");
        }
    }

    private static void comprobarRed(int latencia) {
        try (ControladorSimulacion c = crear(latencia)) {
            c.crearProcesoBuffer("P",2,0,2,100,TipoProceso.PRODUCTOR,"B",1,1);
            c.avanzarCiclo(); c.avanzarCiclo();
            if (latencia == 3) {
                VistaBloqueosBuffer vista = c.consultarBloqueosBuffers()[0];
                verificar(vista.ciclosRed() == 1 && vista.abiertasRed() == 1,
                        "La red no incluyo el ciclo de inicio");
            }
            for (int i = 2; i < 6 + latencia; i++) c.avanzarCiclo();
            VistaBloqueosBuffer fin = c.consultarBloqueosBuffers()[0];
            verificar(fin.ciclosRed() == latencia && fin.esperasRed() == (latencia == 0 ? 0 : 1)
                    && fin.abiertasRed() == 0 && fin.ciclosSemaforo() == 0 && fin.esperasSemaforo() == 0,
                    "Mezclo tiempos de red y semaforo");
            verificar(c.consultarBloqueosProcesos()[0].ciclosRed() == latencia,
                    "El proceso no conserva el tiempo de red");
        }
    }

    private static void comprobarTotalesRemotos() throws Exception {
        try (ControladorSimulacion c = new ControladorSimulacion()) {
            c.cargarCarga(ArchivoConfiguracion.cargar(Path.of("ejemplos/productor-consumidor-remoto.csv")));
            for (int i = 0; i < 100; i++) {
                c.avanzarCiclo();
                VistaBloqueosBuffer b = c.consultarBloqueosBuffers()[0];
                long semaforo = 0, red = 0, episodios = 0, abiertos = 0;
                for (VistaBloqueoProceso p : c.consultarBloqueosProcesos()) {
                    semaforo += p.ciclosSemaforo(); red += p.ciclosRed(); episodios += p.esperasSemaforo();
                    if (p.esperaActual() == MotivoBloqueo.SEMAFORO) abiertos++;
                }
                verificar(b.ciclosSemaforo() == semaforo && b.ciclosRed() == red
                        && b.esperasSemaforo() == episodios && b.abiertasSemaforo() == abiertos,
                        "Los acumulados por proceso no coinciden con el buffer");
            }
            VistaBloqueosBuffer fin = c.consultarBloqueosBuffers()[0];
            verificar(fin.ciclosRed() == 9 && fin.esperasRed() == 3 && fin.promedioRed() == 3
                    && fin.ciclosSemaforo() > 0 && fin.abiertasSemaforo() == 0,
                    "Tiempos finales remotos incorrectos");
            verificar(c.consultarBloqueosProcesos().length == 2, "Incluyo CPU-bound sin buffer o duplico procesos");
            c.configurar(new Configuracion(new int[]{32,32},1,4,0)); c.crearBuffer("B",1,1);
            verificar(c.consultarBloqueosBuffers()[0].esperasSemaforo() == 0
                    && c.consultarBloqueosProcesos().length == 0, "Conservo tiempos de otra sesion");
        }
    }
}
