package Pruebas;

import Modelo.*;
import Sistema.Computador;
import Sistema.ControladorSimulacion;
import Sistema.InterconexionBuffers;
import static Pruebas.PruebaSemaforos.verificar;
import static Pruebas.PruebaSemaforos.rechazar;

/** Limpieza por etapa, esperas multiples y transferencias en carrera. @author Luciano Rojas */
public final class PruebaLimpiezaProductorConsumidor {
    public static void main(String[] args) throws Exception {
        comprobarCancelaciones();
        comprobarVencimientos();
        comprobarNuevosYRed();
        comprobarColasMultiples(false);
        comprobarColasMultiples(true);
        comprobarTransferencia(false);
        comprobarTransferenciaMutex();
        for (int i = 0; i < 24; i++) comprobarTransferencia(true);
        System.out.println("OK: limpieza por etapa, RAM, FIFO, permisos tardios y cancelacion concurrente de transferencias");
    }

    private static ControladorSimulacion crear(int latencia) {
        ControladorSimulacion c = new ControladorSimulacion();
        c.configurar(new Configuracion(new int[] {32,32,32},1,4,latencia));
        c.crearBuffer("B",1,1);
        return c;
    }

    private static Proceso crearProceso(ControladorSimulacion c, TipoProceso tipo, int equipo, int plazo, int objetivo) {
        return c.crearProcesoBuffer(tipo.name(),equipo,0,2,plazo,tipo,"B",1,objetivo);
    }

    private static void sembrar(ControladorSimulacion c) {
        crearProceso(c,TipoProceso.PRODUCTOR,1,100,1);
        for (int i = 0; i < 6; i++) c.avanzarCiclo();
    }

    private static void comprobarCancelaciones() {
        for (TipoProceso tipo : new TipoProceso[] {TipoProceso.PRODUCTOR,TipoProceso.CONSUMIDOR}) {
            for (int paso = 0; paso <= 6; paso++) {
                try (ControladorSimulacion c = crear(2)) {
                    if (tipo == TipoProceso.CONSUMIDOR) sembrar(c);
                    Proceso p = crearProceso(c,tipo,2,100,1);
                    for (int i = 0; i < 30 && p.getPc() < paso; i++) c.avanzarCiclo();
                    verificar(p.getPc() == paso, "No alcanzo la etapa de cancelacion");
                    verificar(c.cancelarProceso(p) == (paso < 6), "Resultado incorrecto de cancelacion");
                    verificar(!c.cancelarProceso(p), "Cancelo un proceso dos veces");
                    comprobarCierre(c,p,tipo,paso >= 4,paso == 6);
                    verificar(p.getContextoBuffer().abandonar().length == 0, "Devolvio permisos otra vez");
                    rechazar(() -> p.getContextoBuffer().conceder(p.getBuffer().getVacios()));
                    rechazar(() -> p.getContextoBuffer().iniciarEspera());
                    rechazar(() -> p.getContextoBuffer().avanzarIntervalo());
                    for (int i = 0; i < 3; i++) c.avanzarCiclo();
                    verificar(p.getPc() == paso, "Un cancelado volvio a ejecutar");
                }
            }
        }
    }

    private static void comprobarVencimientos() {
        for (int latencia : new int[] {0,2}) {
            for (TipoProceso tipo : new TipoProceso[] {TipoProceso.PRODUCTOR,TipoProceso.CONSUMIDOR}) {
                for (int plazo = 1; plazo <= 6 + latencia; plazo++) {
                    try (ControladorSimulacion c = crear(latencia)) {
                        if (tipo == TipoProceso.CONSUMIDOR) sembrar(c);
                        Proceso p = crearProceso(c,tipo,2,plazo,1);
                        for (int i = 0; i < plazo; i++) c.avanzarCiclo();
                        comprobarCierre(c,p,tipo,p.getPc() >= 4,p.getPc() == 6);
                        int pc = p.getPc();
                        for (int i = 0; i < 3; i++) c.avanzarCiclo();
                        verificar(p.getPc() == pc, "Un vencido ejecuto despues de terminar");
                    }
                }
            }
        }
    }

    private static void comprobarCierre(ControladorSimulacion c, Proceso p, TipoProceso tipo,
            boolean modificado, boolean completado) {
        Buffer<?> b = p.getBuffer();
        int cantidad = tipo == TipoProceso.PRODUCTOR ? (modificado ? 1 : 0) : (modificado ? 0 : 1);
        ContextoBuffer contexto = p.getContextoBuffer();
        verificar(p.getEstado() == EstadoProceso.TERMINADO && p.getMotivoBloqueo() == MotivoBloqueo.NINGUNO,
                "El proceso no termino limpiamente");
        verificar(contexto.estaLimpiado() && contexto.estaCancelado() != completado
                && !contexto.tieneRecurso() && !contexto.tieneMutex()
                && !contexto.isRedPreparada() && contexto.getRedHasta() == -1, "Quedaron reservas o espera de red");
        verificar(b.getCantidad() == cantidad && b.getLlenos().getValor() == cantidad
                && b.getVacios().getValor() == 1-cantidad && b.getMutexSimulado().getValor() == 1,
                "Cambio el contenido o se perdio un permiso en " + tipo + " PC " + p.getPc());
        verificar(b.getMutexSimulado().getEsperando().length == 0 && b.getVacios().getEsperando().length == 0
                && b.getLlenos().getEsperando().length == 0, "Quedo una espera del proceso terminado");
        verificar(c.getComputadores()[1].getMemoria().getUsada() == 0
                && c.getComputadores()[0].getMemoria().getUsada() == 1, "RAM de proceso o buffer incorrecta");
    }

    private static void comprobarNuevosYRed() {
        try (ControladorSimulacion c = crear(20)) {
            Proceso ocupa = c.crearProceso("Ocupa RAM",2,0,32,100,500);
            Proceso nuevo = crearProceso(c,TipoProceso.PRODUCTOR,2,100,1);
            verificar(nuevo.getEstado() == EstadoProceso.NUEVO, "No quedo esperando RAM");
            verificar(c.cancelarProceso(nuevo) && c.getComputadores()[1].getMemoria().getUsada() == 32,
                    "Cancelo una reserva ajena al retirar un nuevo");
            c.cancelarProceso(ocupa);
            Proceso red = crearProceso(c,TipoProceso.PRODUCTOR,2,100,1);
            c.avanzarCiclo(); c.avanzarCiclo();
            verificar(red.getMotivoBloqueo() == MotivoBloqueo.RED, "No inicio la espera de red");
            c.cancelarProceso(red);
            for (int i = 0; i < 25; i++) c.avanzarCiclo();
            comprobarCierre(c,red,TipoProceso.PRODUCTOR,false,false);
            verificar(red.getPc() == 1 && c.getComputadores()[1].getBloqueados().length == 0,
                    "La red desperto un proceso cancelado");
            rechazar(() -> c.cancelarProceso(null));
        }
    }

    private static void comprobarColasMultiples(boolean lleno) {
        try (ControladorSimulacion c = crear(0)) {
            if (lleno) sembrar(c);
            TipoProceso tipo = lleno ? TipoProceso.PRODUCTOR : TipoProceso.CONSUMIDOR;
            Proceso cancelado = crearProceso(c,tipo,2,500,1);
            Proceso sigue = crearProceso(c,tipo,3,500,1);
            c.avanzarCiclo(); c.avanzarCiclo();
            SemaforoSimulado semaforo = lleno ? cancelado.getBuffer().getVacios() : cancelado.getBuffer().getLlenos();
            verificar(semaforo.getEsperando().length == 2, "No encolo ambos procesos");
            c.cancelarProceso(cancelado);
            verificar(semaforo.getEsperando().length == 1 && semaforo.getEsperando()[0] == sigue,
                    "Cancelar altero la cola del superviviente");
            crearProceso(c,lleno ? TipoProceso.CONSUMIDOR : TipoProceso.PRODUCTOR,1,500,lleno ? 2 : 1);
            for (int i = 0; i < 100 && sigue.getEstado() != EstadoProceso.TERMINADO; i++) c.avanzarCiclo();
            verificar(sigue.getEstado() == EstadoProceso.TERMINADO && sigue.getContextoBuffer().getCompletados() == 1,
                    "La limpieza impidio despertar al superviviente");
            verificar(semaforo.getEsperando().length == 0, "Quedaron esperas al terminar");
        }
    }

    private static void comprobarTransferencia(boolean concurrente) throws Exception {
        InterconexionBuffers bus = new InterconexionBuffers();
        Computador anfitrion = new Computador(1,32,bus), remoto = new Computador(2,32,bus);
        bus.conectar(new Computador[] {anfitrion,remoto});
        Buffer<Object> b = new Buffer<>("B",1,1);
        ParametrosBuffer parametros = new ParametrosBuffer("B",1,1);
        Proceso productor = new Proceso("P",1,TipoProceso.PRODUCTOR,0,2,6,100,parametros,b);
        Proceso consumidor = new Proceso("C",2,TipoProceso.CONSUMIDOR,0,2,6,100,parametros,b);
        anfitrion.recibirProceso(productor); remoto.recibirProceso(consumidor);
        anfitrion.admitirProcesos(); remoto.admitirProcesos();
        for (int ciclo = 1; ciclo <= 5; ciclo++) {
            anfitrion.avanzarCiclo(ciclo); remoto.avanzarCiclo(ciclo); bus.finalizarCiclo(ciclo);
        }
        anfitrion.avanzarCiclo(6); // Signal extrae el receptor pero aun no cruza la barrera.
        verificar(bus.getCantidadPendientes() == 1, "Falta la transferencia pendiente");
        if (!concurrente) {
            remoto.terminarProceso(consumidor);
            verificar(bus.getCantidadPendientes() == 0 && b.getLlenos().getValor() == 1,
                    "No retiro ni devolvio la transferencia antes de la barrera");
            bus.finalizarCiclo(6);
        } else {
            java.util.concurrent.CountDownLatch inicio = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<Throwable> fallo = new java.util.concurrent.atomic.AtomicReference<>();
            Thread cancela = hilo(inicio,fallo,() -> remoto.terminarProceso(consumidor));
            Thread entrega = hilo(inicio,fallo,() -> bus.finalizarCiclo(6));
            cancela.start(); entrega.start(); inicio.countDown();
            cancela.join(2000); entrega.join(2000);
            verificar(!cancela.isAlive() && !entrega.isAlive(), "Interbloqueo entre entrega y cancelacion");
            if (fallo.get() != null) throw new AssertionError("Fallo concurrente",fallo.get());
        }
        verificar(consumidor.getEstado() == EstadoProceso.TERMINADO && bus.getCantidadPendientes() == 0
                && b.getCantidad() == 1 && b.getLlenos().getValor() == 1 && b.getVacios().getValor() == 0
                && b.getMutexSimulado().getValor() == 1, "Permiso perdido o duplicado en transferencia cancelada");
        verificar(!remoto.terminarProceso(consumidor), "Termino dos veces el mismo proceso");
    }

    private static Thread hilo(java.util.concurrent.CountDownLatch inicio,
            java.util.concurrent.atomic.AtomicReference<Throwable> fallo, Runnable accion) {
        Thread hilo = new Thread(() -> {
            try { inicio.await(); accion.run(); }
            catch (Throwable error) { fallo.set(error); }
        });
        hilo.setDaemon(true);
        return hilo;
    }

    private static void comprobarTransferenciaMutex() {
        InterconexionBuffers bus = new InterconexionBuffers();
        Computador anfitrion = new Computador(1,32,bus), remoto = new Computador(2,32,bus);
        bus.conectar(new Computador[] {anfitrion,remoto});
        Buffer<Object> b = new Buffer<>("B",2,1);
        ParametrosBuffer parametros = new ParametrosBuffer("B",1,1);
        Proceso titular = new Proceso("Titular",1,TipoProceso.PRODUCTOR,0,2,6,100,parametros,b);
        Proceso cancelado = new Proceso("Espera mutex",2,TipoProceso.PRODUCTOR,0,2,6,100,parametros,b);
        anfitrion.recibirProceso(titular); remoto.recibirProceso(cancelado);
        anfitrion.admitirProcesos(); remoto.admitirProcesos();
        for (int ciclo = 1; ciclo <= 4; ciclo++) {
            anfitrion.avanzarCiclo(ciclo); remoto.avanzarCiclo(ciclo); bus.finalizarCiclo(ciclo);
        }
        verificar(cancelado.getContextoBuffer().tieneRecurso() && b.getMutexSimulado().getEsperando().length == 1,
                "Falta la espera de mutex con espacio adquirido");
        anfitrion.avanzarCiclo(5);
        verificar(bus.getCantidadPendientes() == 1, "No encolo el permiso de mutex remoto");
        remoto.terminarProceso(cancelado);
        remoto.avanzarCiclo(5);
        bus.finalizarCiclo(5);
        anfitrion.avanzarCiclo(6); remoto.avanzarCiclo(6); bus.finalizarCiclo(6);
        verificar(bus.getCantidadPendientes() == 0 && b.getCantidad() == 1 && b.getVacios().getValor() == 1
                && b.getLlenos().getValor() == 1 && b.getMutexSimulado().getValor() == 1,
                "No devolvio el espacio reservado y el mutex en transito");
        verificar(((ContextoBuffer.Elemento) b.instantanea()[0]).productor() == titular.getId(),
                "La cancelacion cambio el elemento del titular");
    }
}
