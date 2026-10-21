package Pruebas;

import Interfaces.PanelProductorConsumidor;
import Modelo.*;
import Persistencia.ArchivoConfiguracion;
import Sistema.Computador;
import Sistema.ControladorSimulacion;
import java.nio.file.Files;
import java.nio.file.Path;
import static Pruebas.PruebaSemaforos.verificar;
import static Pruebas.PruebaSemaforos.rechazar;

/** Escenarios deterministas del motor y sus invariantes. @author Luciano Rojas */
public final class PruebaProductorConsumidorLocal {
    public static void main(String[] args) throws Exception {
        comprobarCarga();
        comprobarCiclos();
        comprobarMultiples();
        comprobarBufferLleno();
        comprobarVencimientos();
        comprobarEsperasVencidas();
        comprobarReloj();
        System.out.println("OK: productor consumidor local, ciclos, apropiacion, invariantes, vencimientos, CSV y reloj");
    }

    private static ControladorSimulacion crear() {
        ControladorSimulacion c = new ControladorSimulacion();
        c.configurar(new Configuracion(new int[] {64,64}, 1, 4, 0));
        return c;
    }

    private static Proceso proceso(ControladorSimulacion c, TipoProceso tipo, int intervalo, int elementos, int plazo) {
        return c.crearProcesoBuffer(tipo.name(), 1, 0, 2, plazo, tipo, "B", intervalo, elementos);
    }

    private static void comprobarCarga() throws Exception {
        rechazar(() -> new ParametrosBuffer("B", Integer.MAX_VALUE, 1));
        rechazar(() -> new ParametrosBuffer("B", 1, 0));
        String[][] invalidos = {
            null, {}, {"P","1","0","2","500","PRODUCTOR","B","0","1"},
            {"P","1","0","2","500","PRODUCTOR","B","1.5","1"},
            {"P","1","0","2","500","CPU_BOUND","B","1","1"},
            {"P","1","0","2","500","PRODUCTOR",null,"1","1"},
            {"P","1","-1","2","500","PRODUCTOR","B","1","1"}
        };
        for (String[] campos : invalidos) rechazar(() -> PanelProductorConsumidor.validarCampos(campos));
        try (ControladorSimulacion c = crear()) {
            c.crearBuffer("B", 1, 1);
            rechazar(() -> c.crearProcesoBuffer("Remoto",2,0,1,100,TipoProceso.PRODUCTOR,"B",1,1));
            rechazar(() -> c.crearProcesoBuffer("Ausente",1,0,1,100,TipoProceso.PRODUCTOR,"No existe",1,1));
            verificar(c.getCargaInicial().getProcesos().length == 0 && c.getComputadores()[0].getMemoria().getUsada() == 1,
                    "Solicitud invalida modifico recursos");
            String nombre = "Datos,\n\"Compartidos\"";
            c.crearBuffer(nombre, 2, 1);
            Proceso original = c.crearProcesoBuffer("P",1,0,2,100,TipoProceso.PRODUCTOR,nombre,2,3);
            c.getComputadores()[0].avanzarCiclo(1);
            Path archivo = Files.createTempFile("avilaos-local-", ".csv");
            try {
                ArchivoConfiguracion.guardar(archivo, c.getCargaInicial());
                verificar(Persistencia.Csv.leer(Files.readString(archivo))[0].campos()[1].equals("2"), "No uso CSV v2");
                c.cargarCarga(ArchivoConfiguracion.cargar(archivo));
            } finally { Files.deleteIfExists(archivo); }
            Proceso nuevo = c.getComputadores()[0].getListos()[0];
            verificar(nuevo.getId() != original.getId() && nuevo.getPc() == 0
                    && nuevo.getParametrosBuffer().equals(original.getParametrosBuffer())
                    && nuevo.getBuffer().getCantidad() == 0, "No reconstruyo la carga inicial");
            c.cargarCarga(ArchivoConfiguracion.cargar(Path.of("ejemplos/cluster-inicial.csv")));
            verificar(c.getCargaInicial().getProcesos()[0].tipo() == TipoProceso.CPU_BOUND, "Rompio CSV v1");
            c.cargarCarga(ArchivoConfiguracion.cargar(Path.of("ejemplos/productor-consumidor-local.csv")));
            for (int ciclo = 1; ciclo <= 100; ciclo++)
                for (Computador equipo : c.getComputadores()) equipo.avanzarCiclo(ciclo);
            verificar(c.getComputadores()[0].getTerminados().length == 2
                    && c.getComputadores()[1].getTerminados().length == 1
                    && c.consultarBuffers()[0].cantidad() == 0, "El ejemplo no completo");
        }
    }

    private static void comprobarCiclos() {
        try (ControladorSimulacion c = crear()) {
            Buffer<Object> b = c.crearBuffer("B",1,1);
            Proceso consumidor = proceso(c,TipoProceso.CONSUMIDOR,1,3,500);
            Proceso productor = proceso(c,TipoProceso.PRODUCTOR,1,3,500);
            Proceso[] procesos = {consumidor, productor};
            Computador equipo = c.getComputadores()[0];
            int sistema = 0, usuario = 0;
            boolean vacio = false;
            for (int ciclo = 1; ciclo <= 100 && equipo.getTerminados().length < 2; ciclo++) {
                int antes = consumidor.getPc() + productor.getPc();
                equipo.avanzarCiclo(ciclo);
                int delta = consumidor.getPc() + productor.getPc() - antes;
                verificar(delta >= 0 && delta <= 1, "Ejecuto mas de una instruccion por ciclo");
                if (delta == 1) {
                    if (equipo.getModoCPU() == ModoCPU.SISTEMA_OPERATIVO) sistema++;
                    else usuario++;
                }
                vacio |= b.getLlenos().getEsperando().length > 0;
                invariantes(b,procesos);
            }
            verificar(equipo.getTerminados().length == 2 && vacio && sistema == 30 && usuario == 6,
                    "Ciclos o bloqueo por vacio incorrectos");
            verificar(b.getCantidad() == 0 && equipo.getMemoria().getUsada() == 1, "Contenido o RAM final incorrectos");
        }
    }

    private static void comprobarMultiples() {
        try (ControladorSimulacion c = crear()) {
            Buffer<Object> b = c.crearBuffer("B",2,1);
            Proceso[] procesos = {
                proceso(c,TipoProceso.PRODUCTOR,1,4,5000), proceso(c,TipoProceso.PRODUCTOR,2,4,5000),
                proceso(c,TipoProceso.CONSUMIDOR,1,4,5000), proceso(c,TipoProceso.CONSUMIDOR,2,4,5000)
            };
            Computador equipo = c.getComputadores()[0];
            boolean esperaMutex = false;
            for (int ciclo = 1; ciclo <= 500 && equipo.getTerminados().length < 4; ciclo++) {
                equipo.avanzarCiclo(ciclo);
                esperaMutex |= b.getMutexSimulado().getEsperando().length > 0;
                invariantes(b,procesos);
                equipo.cederCPU(); // Fuerza cambios de contexto tambien dentro de la region critica.
            }
            verificar(esperaMutex && equipo.getTerminados().length == 4 && b.getCantidad() == 0,
                    "No completo varios productores/consumidores con apropiacion");
            for (Proceso p : procesos)
                verificar(p.getPc() == p.getInstrucciones() && p.getContextoBuffer().getCompletados() == 4,
                        "Repitio o perdio instrucciones al retomar");
        }
    }

    private static void comprobarBufferLleno() {
        try (ControladorSimulacion c = crear()) {
            Buffer<Object> b = c.crearBuffer("B",1,1);
            Proceso p = proceso(c,TipoProceso.PRODUCTOR,1,2,500);
            Proceso cpu = c.crearProceso("CPU",1,0,2,2,500);
            Computador equipo = c.getComputadores()[0];
            for (int ciclo = 1; ciclo <= 10; ciclo++) equipo.avanzarCiclo(ciclo);
            verificar(p.getEstado() == EstadoProceso.BLOQUEADO && b.getVacios().getEsperando().length == 1
                    && cpu.getEstado() == EstadoProceso.TERMINADO, "Buffer lleno detuvo la CPU");
            proceso(c,TipoProceso.CONSUMIDOR,1,2,500);
            for (int ciclo = 11; ciclo <= 50 && equipo.getTerminados().length < 3; ciclo++) equipo.avanzarCiclo(ciclo);
            verificar(equipo.getTerminados().length == 3 && b.getCantidad() == 0, "No desperto al productor");
        }
    }

    private static void comprobarVencimientos() {
        for (TipoProceso tipo : new TipoProceso[] {TipoProceso.PRODUCTOR, TipoProceso.CONSUMIDOR}) {
            for (int plazo = 1; plazo <= 6; plazo++) {
                try (ControladorSimulacion c = crear()) {
                    Buffer<Object> b = c.crearBuffer("B",1,1);
                    Computador equipo = c.getComputadores()[0];
                    int inicio = 0;
                    if (tipo == TipoProceso.CONSUMIDOR) {
                        proceso(c,TipoProceso.PRODUCTOR,1,1,100);
                        for (int i = 1; i <= 6; i++) equipo.avanzarCiclo(i);
                        inicio = 6;
                    }
                    Proceso p = proceso(c,tipo,1,1,plazo);
                    for (int i = 1; i <= plazo; i++) equipo.avanzarCiclo(inicio+i);
                    int cantidad = tipo == TipoProceso.PRODUCTOR ? (plazo >= 4 ? 1 : 0) : (plazo >= 4 ? 0 : 1);
                    verificar(p.getEstado() == EstadoProceso.TERMINADO && b.getCantidad() == cantidad
                            && b.getLlenos().getValor() == cantidad && b.getVacios().getValor() == 1-cantidad
                            && b.getMutexSimulado().getValor() == 1 && equipo.getMemoria().getUsada() == 1,
                            "Fuga al vencer " + tipo + " en ciclo " + plazo);
                }
            }
        }
    }

    private static void invariantes(Buffer<?> b, Proceso[] procesos) {
        int espaciosRetenidos = 0, elementosRetenidos = 0, mutexRetenidos = 0;
        for (Proceso p : procesos) {
            ContextoBuffer contexto = p.getContextoBuffer();
            if (contexto.tieneMutex()) mutexRetenidos++;
            if (!contexto.tieneRecurso()) continue;
            boolean espacio = (p.getTipo() == TipoProceso.PRODUCTOR) != contexto.estaModificado();
            if (espacio) espaciosRetenidos++; else elementosRetenidos++;
        }
        verificar(b.getCantidad() >= 0 && b.getCantidad() <= b.getCapacidad(), "Capacidad fuera de rango");
        verificar(b.getMutexSimulado().getValor() + mutexRetenidos == 1, "Mutex duplicado o perdido");
        verificar(b.getVacios().getValor() + espaciosRetenidos + b.getCantidad() == b.getCapacidad(), "Espacios perdidos");
        verificar(b.getLlenos().getValor() + elementosRetenidos == b.getCantidad(), "Elementos perdidos");
    }

    private static void comprobarEsperasVencidas() {
        try (ControladorSimulacion c = crear()) {
            Buffer<Object> b = c.crearBuffer("B",2,1);
            Proceso primero = proceso(c,TipoProceso.PRODUCTOR,1,1,100);
            Proceso vence = proceso(c,TipoProceso.PRODUCTOR,1,1,6);
            Computador equipo = c.getComputadores()[0];
            for (int ciclo = 1; ciclo <= 12; ciclo++) {
                equipo.avanzarCiclo(ciclo);
                equipo.cederCPU();
                invariantes(b,new Proceso[] {primero,vence});
            }
            verificar(vence.getPc() == 3 && vence.getEstado() == EstadoProceso.TERMINADO
                    && b.getMutexSimulado().getEsperando().length == 0 && b.getCantidad() == 1,
                    "No limpio la espera de mutex con espacio reservado");
        }
        try (ControladorSimulacion c = crear()) {
            Buffer<Object> b = c.crearBuffer("B",1,1);
            Proceso vence = proceso(c,TipoProceso.CONSUMIDOR,1,1,8);
            Proceso productor = proceso(c,TipoProceso.PRODUCTOR,1,1,100);
            Computador equipo = c.getComputadores()[0];
            for (int ciclo = 1; ciclo <= 8; ciclo++) {
                equipo.avanzarCiclo(ciclo);
                invariantes(b,new Proceso[] {vence,productor});
            }
            verificar(vence.getEstado() == EstadoProceso.TERMINADO && vence.getPc() == 2
                    && b.getCantidad() == 1 && b.getLlenos().getValor() == 1,
                    "Perdio el permiso entregado en el mismo ciclo del vencimiento");
        }
    }

    private static void comprobarReloj() throws Exception {
        try (ControladorSimulacion c = crear()) {
            c.crearBuffer("B",1,1);
            proceso(c,TipoProceso.CONSUMIDOR,1,10,5000);
            proceso(c,TipoProceso.PRODUCTOR,1,10,5000);
            c.iniciar();
            long limite = System.nanoTime() + 3_000_000_000L;
            while (c.getCiclo() < 3 && System.nanoTime() < limite) Thread.sleep(2);
            c.pausar();
            long ciclo = c.getCiclo();
            Thread.sleep(10);
            verificar(ciclo == c.getCiclo(), "El reloj siguio durante la pausa");
            c.reanudar();
            while (c.getComputadores()[0].getTerminados().length < 2 && System.nanoTime() < limite) Thread.sleep(2);
            c.detener();
            verificar(c.getComputadores()[0].getTerminados().length == 2 && !c.getEstado().startsWith("Error"),
                    "Fallo el reloj con productores consumidores");
            verificar(c.getComputadores()[0].getUltimoCiclo() == c.getComputadores()[1].getUltimoCiclo(),
                    "Los computadores quedaron en ciclos distintos");
        }
    }
}
