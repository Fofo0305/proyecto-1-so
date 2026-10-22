package Pruebas;

import Modelo.*;
import Sistema.ControladorSimulacion;
import Persistencia.ArchivoConfiguracion;
import java.nio.file.Files;
import java.nio.file.Path;
import static Pruebas.PruebaSemaforos.verificar;

/** Acumulados independientes del historial, los refrescos y la finalizacion. @author Luciano Rojas */
public final class PruebaContadoresBuffers {
    public static void main(String[] args) throws Exception {
        comprobarAlmacenamiento();
        comprobarConcurrencia();
        comprobarVencimiento();
        comprobarCargaRemota();
        System.out.println("OK: contadores exactos, lecturas coherentes, concurrencia, limpieza y reinicio CSV");
    }

    private static void comprobarAlmacenamiento() {
        Buffer<Integer> buffer = new Buffer<>("B",1,1);
        verificar(buffer.extraer() == null && buffer.consultarContadores().consumidos() == 0,
                "Conto una extraccion fallida");
        buffer.insertar(1);
        verificar(!buffer.insertar(2) && buffer.consultarContadores().producidos() == 1,
                "Conto una insercion fallida");
        VistaContadoresBuffer anterior = buffer.consultarContadores();
        buffer.extraer();
        for (int i = 0; i < 400; i++) { buffer.insertar(i); buffer.extraer(); }
        VistaContadoresBuffer actual = buffer.consultarContadores();
        verificar(actual.producidos() == 401 && actual.consumidos() == 401 && actual.cantidad() == 0,
                "No acumulo todas las modificaciones");
        verificar(anterior.producidos() == 1 && anterior.consumidos() == 0 && anterior.cantidad() == 1,
                "Se modifico una instantanea anterior");
    }

    private static void comprobarConcurrencia() throws Exception {
        Buffer<Integer> buffer = new Buffer<>("Concurrente",7,1);
        java.util.concurrent.atomic.AtomicReference<Throwable> fallo = new java.util.concurrent.atomic.AtomicReference<>();
        long limite = System.nanoTime() + 5_000_000_000L;
        Thread productor = new Thread(() -> {
            for (int i = 0; i < 2000; i++) {
                while (!buffer.insertar(i)) {
                    if (System.nanoTime() >= limite) throw new AssertionError("Productor detenido");
                    Thread.yield();
                }
            }
        });
        Thread consumidor = new Thread(() -> {
            for (int i = 0; i < 2000; i++) {
                Integer valor;
                while ((valor = buffer.extraer()) == null) {
                    if (System.nanoTime() >= limite) throw new AssertionError("Consumidor detenido");
                    Thread.yield();
                }
                verificar(valor == i,"Orden FIFO incorrecto");
            }
        });
        productor.setDaemon(true); consumidor.setDaemon(true);
        productor.setUncaughtExceptionHandler((h,e) -> fallo.set(e));
        consumidor.setUncaughtExceptionHandler((h,e) -> fallo.set(e));
        productor.start(); consumidor.start();
        do {
            VistaContadoresBuffer vista = buffer.consultarContadores();
            verificar(vista.producidos() - vista.consumidos() == vista.cantidad(),
                    "La ocupacion no corresponde a los acumulados");
        } while ((productor.isAlive() || consumidor.isAlive()) && System.nanoTime() < limite);
        productor.join(1000); consumidor.join(1000);
        verificar(!productor.isAlive() && !consumidor.isAlive(),"Transferencias sin completar");
        if (fallo.get() != null) throw new AssertionError("Fallo concurrente",fallo.get());
        verificar(buffer.consultarContadores().producidos() == 2000
                && buffer.consultarContadores().consumidos() == 2000, "Se perdieron acumulados concurrentes");
    }

    private static void comprobarVencimiento() {
        try (ControladorSimulacion c = new ControladorSimulacion()) {
            c.configurar(new Configuracion(new int[]{32,32},1,4,0)); c.crearBuffer("B",1,1);
            Proceso productor = c.crearProcesoBuffer("P",1,0,2,4,TipoProceso.PRODUCTOR,"B",1,1);
            for (int i = 0; i < 4; i++) c.avanzarCiclo();
            verificar(productor.getContextoBuffer().getCompletados() == 0
                    && c.consultarContadoresBuffers()[0].producidos() == 1, "Perdio la produccion al vencer");
            Proceso consumidor = c.crearProcesoBuffer("C",2,0,2,4,TipoProceso.CONSUMIDOR,"B",1,1);
            for (int i = 0; i < 4; i++) c.avanzarCiclo();
            verificar(consumidor.getContextoBuffer().getCompletados() == 0
                    && c.consultarContadoresBuffers()[0].consumidos() == 1, "Perdio el consumo al vencer");
            verificar(!c.cancelarProceso(productor) && !c.cancelarProceso(consumidor),"Repitio la limpieza");
            for (int i = 0; i < 20; i++) {
                VistaContadoresBuffer vista = c.consultarContadoresBuffers()[0];
                c.consultarEventosBuffers(); c.consultarBuffers();
                verificar(vista.producidos() == 1 && vista.consumidos() == 1 && vista.cantidad() == 0,
                        "Consulta o limpieza duplico modificaciones");
            }
        }
    }

    private static void comprobarCargaRemota() throws Exception {
        try (ControladorSimulacion c = new ControladorSimulacion()) {
            c.cargarCarga(ArchivoConfiguracion.cargar(Path.of("ejemplos/productor-consumidor-remoto.csv")));
            for (int i = 0; i < 100; i++) c.avanzarCiclo();
            VistaContadoresBuffer vista = c.consultarContadoresBuffers()[0];
            verificar(vista.producidos() == 3 && vista.consumidos() == 3 && vista.cantidad() == 0,
                    "Conteo remoto incorrecto");
            Path archivo = Files.createTempFile("contadores-buffers-", ".csv");
            try {
                ArchivoConfiguracion.guardar(archivo,c.getCargaInicial());
                c.cargarCarga(ArchivoConfiguracion.cargar(archivo));
                verificar(c.consultarContadoresBuffers()[0].producidos() == 0
                        && c.consultarContadoresBuffers()[0].consumidos() == 0, "CSV reanudo contadores de ejecucion");
                verificar(vista.producidos() == 3,"El reinicio altero la lectura anterior");
            } finally { Files.deleteIfExists(archivo); }
        }
    }
}
