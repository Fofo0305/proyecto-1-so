package Pruebas;

import Modelo.*;
import Persistencia.ArchivoConfiguracion;
import Sistema.ControladorSimulacion;
import java.nio.file.Path;
import static Pruebas.PruebaSemaforos.verificar;

/** Acotacion, instantaneas y eventos emitidos por el motor. @author Luciano Rojas */
public final class PruebaEventosBuffers {
    public static void main(String[] args) throws Exception {
        comprobarHistorial();
        comprobarFlujo();
        comprobarVencimiento();
        System.out.println("OK: eventos acotados, copias inmutables, concurrencia y registro sin duplicados");
    }

    private static void comprobarHistorial() throws Exception {
        RegistroEventosBuffer registro = new RegistroEventosBuffer("B",2);
        registro.registrar(1,1,1,TipoEventoBuffer.PRODUCCION,"uno");
        VistaEventosBuffer anterior = registro.consultar();
        registro.registrar(2,1,1,TipoEventoBuffer.PRODUCCION,"dos");
        registro.registrar(3,1,1,TipoEventoBuffer.CONSUMO,"tres");
        VistaEventosBuffer vista = registro.consultar();
        verificar(vista.descartados() == 1 && vista.eventos().length == 2
                && vista.eventos()[0].secuencia() == 2 && vista.eventos()[1].secuencia() == 3,
                "El historial no conservo los dos ultimos eventos");
        EventoBuffer[] copia = vista.eventos(); copia[0] = null;
        verificar(vista.eventos()[0] != null && anterior.eventos().length == 1
                && anterior.eventos()[0].secuencia() == 1, "La instantanea cambio desde fuera");
        java.util.concurrent.atomic.AtomicReference<Throwable> fallo = new java.util.concurrent.atomic.AtomicReference<>();
        Thread[] hilos = new Thread[4];
        for (int i = 0; i < hilos.length; i++) {
            hilos[i] = new Thread(() -> {
                try { for (int j = 0; j < 100; j++) registro.registrar(4,1,1,TipoEventoBuffer.PRODUCCION,"concurrente"); }
                catch (Throwable error) { fallo.set(error); }
            });
            hilos[i].setDaemon(true); hilos[i].start();
        }
        for (Thread hilo : hilos) { hilo.join(3000); verificar(!hilo.isAlive(), "Historial detenido"); }
        if (fallo.get() != null) throw new AssertionError("Fallo concurrente",fallo.get());
        vista = registro.consultar();
        verificar(vista.descartados() == 401 && vista.eventos()[0].secuencia() == 402
                && vista.eventos()[1].secuencia() == 403, "Se perdieron secuencias concurrentes");
    }

    private static void comprobarFlujo() throws Exception {
        try (ControladorSimulacion c = new ControladorSimulacion()) {
            c.cargarCarga(ArchivoConfiguracion.cargar(Path.of("ejemplos/productor-consumidor-remoto.csv")));
            for (int i = 0; i < 100; i++) c.avanzarCiclo();
            VistaEventosBuffer vista = c.consultarEventosBuffers()[0];
            verificar(contar(vista,TipoEventoBuffer.PRODUCCION) == 3 && contar(vista,TipoEventoBuffer.CONSUMO) == 3,
                    "Se repitieron o perdieron operaciones en el registro");
            verificar(contar(vista,TipoEventoBuffer.ACCESO_REMOTO) == 3
                    && contar(vista,TipoEventoBuffer.BLOQUEO_RED) == 3
                    && contar(vista,TipoEventoBuffer.DESPERTAR_RED) == 3
                    && contar(vista,TipoEventoBuffer.BLOQUEO_SEMAFORO) > 0
                    && contar(vista,TipoEventoBuffer.DESPERTAR_SEMAFORO) > 0, "Faltan eventos de espera o red");
            long secuencia = 0;
            for (EventoBuffer evento : vista.eventos()) {
                verificar(evento.secuencia() > secuencia && evento.ciclo() > 0 && evento.ciclo() <= c.getCiclo(),
                        "Secuencia o ciclo invalido");
                secuencia = evento.secuencia();
            }
            for (int i = 0; i < 20; i++) {
                c.consultarBuffers(); c.consultarVistas();
                verificar(c.consultarEventosBuffers()[0].eventos().length == vista.eventos().length,
                        "Consultar genero eventos nuevos");
            }
            c.configurar(new Configuracion(new int[]{32,32},1,4,0));
            verificar(c.consultarEventosBuffers().length == 0 && vista.eventos().length > 0,
                    "La sesion nueva conservo registros o altero la copia anterior");
        }
    }

    private static void comprobarVencimiento() {
        try (ControladorSimulacion c = new ControladorSimulacion()) {
            c.configurar(new Configuracion(new int[]{32,32},1,4,0));
            c.crearBuffer("B",1,1);
            Proceso p = c.crearProcesoBuffer("Vence",1,0,2,4,TipoProceso.PRODUCTOR,"B",1,1);
            for (int i = 0; i < 4; i++) c.avanzarCiclo();
            VistaEventosBuffer vista = c.consultarEventosBuffers()[0];
            verificar(contar(vista,TipoEventoBuffer.PRODUCCION) == 1
                    && contar(vista,TipoEventoBuffer.TERMINACION) == 1
                    && contar(vista,TipoEventoBuffer.DEVOLUCION_PERMISO) == 2,
                    "El vencimiento perdio la modificacion o su limpieza");
            for (EventoBuffer evento : vista.eventos()) verificar(evento.ciclo() == 4, "Evento desplazado de ciclo");
            verificar(!c.cancelarProceso(p) && c.consultarEventosBuffers()[0].eventos().length == vista.eventos().length,
                    "La limpieza repetida duplico eventos");
        }
    }

    private static int contar(VistaEventosBuffer vista, TipoEventoBuffer tipo) {
        int total = 0;
        for (EventoBuffer evento : vista.eventos()) if (evento.tipo() == tipo) total++;
        return total;
    }
}
