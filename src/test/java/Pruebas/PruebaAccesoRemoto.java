package Pruebas;

import Modelo.*;
import Persistencia.ArchivoConfiguracion;
import Sistema.Computador;
import Sistema.ControladorSimulacion;
import java.nio.file.Files;
import java.nio.file.Path;
import static Pruebas.PruebaSemaforos.verificar;
import static Pruebas.PruebaSemaforos.rechazar;

/** Latencia, permisos remotos y barrera de reloj. @author Luciano Rojas */
public final class PruebaAccesoRemoto {
    public static void main(String[] args) throws Exception {
        for (int latencia : new int[] {0,1,3,10}) {
            comprobarDuracion(latencia, false);
            comprobarDuracion(latencia, true);
        }
        comprobarCPUDisponible();
        comprobarCambioYArchivo();
        comprobarEjemplo();
        comprobarVencimiento();
        comprobarCruce(false);
        comprobarCruce(true);
        System.out.println("OK: latencia exacta, CPU disponible, cambio en vivo, CSV, vencimientos y acceso cruzado");
    }

    private static ControladorSimulacion crear(int latencia) {
        ControladorSimulacion c = new ControladorSimulacion();
        c.configurar(new Configuracion(new int[] {64,64},1,4,latencia));
        return c;
    }

    private static Proceso crearProceso(ControladorSimulacion c, String nombre, int computador,
            TipoProceso tipo, String buffer, int elementos, int deadline) {
        return c.crearProcesoBuffer(nombre, computador, 0, 2, deadline, tipo, buffer, 1, elementos);
    }

    private static void comprobarDuracion(int latencia, boolean remoto) {
        try (ControladorSimulacion c = crear(latencia)) {
            Buffer<Object> b = c.crearBuffer("B",2,1);
            Proceso p = crearProceso(c,"P",remoto ? 2 : 1,TipoProceso.PRODUCTOR,"B",2,1000);
            int esperados = 12 + (remoto ? 2 * latencia : 0);
            for (int ciclo = 1; ciclo <= esperados; ciclo++) {
                c.avanzarCiclo();
                if (ciclo < esperados) verificar(p.getEstado() != EstadoProceso.TERMINADO, "Termino antes de la latencia");
            }
            verificar(p.getEstado() == EstadoProceso.TERMINADO && p.getPc() == 12
                    && b.getCantidad() == 2 && b.getLlenos().getValor() == 2
                    && b.getMutexSimulado().getValor() == 1, "Duracion o permisos incorrectos con latencia " + latencia);
            verificar(c.getComputadores()[0].getMemoria().getUsada() == 2
                    && c.getComputadores()[1].getMemoria().getUsada() == 0, "La RAM no quedo en el anfitrion");
        }
    }

    private static void comprobarCPUDisponible() {
        try (ControladorSimulacion c = crear(3)) {
            c.crearBuffer("B",1,1);
            Proceso p = crearProceso(c,"Remoto",2,TipoProceso.PRODUCTOR,"B",1,100);
            Proceso otro = c.crearProceso("CPU",2,0,2,3,100);
            c.avanzarCiclo();
            c.avanzarCiclo();
            verificar(p.getMotivoBloqueo() == MotivoBloqueo.RED && p.getPc() == 1 && otro.getPc() == 1,
                    "El remoto no libero CPU al empezar la espera");
            var vistas = c.consultarVistas()[1];
            verificar(vistas.colaBloqueados()[0].operacion().contains("acceso remoto a C1")
                    && vistas.colaBloqueados()[0].operacion().contains("2 ciclos pendientes"), "Falta detalle de red");
            c.avanzarCiclo();
            verificar(p.getPc() == 1 && otro.getPc() == 2, "El proceso avanzo instrucciones durante la red");
            c.avanzarCiclo();
            verificar(p.getEstado() == EstadoProceso.LISTO && otro.getEstado() == EstadoProceso.TERMINADO,
                    "No concluyo la espera al tercer ciclo");
            c.avanzarCiclo();
            verificar(p.getPc() == 2, "No retomo la etapa pendiente despues de la latencia");
        }
    }

    private static void comprobarCambioYArchivo() throws Exception {
        try (ControladorSimulacion c = crear(5)) {
            c.crearBuffer("B",2,1);
            Proceso p = crearProceso(c,"Remoto",2,TipoProceso.PRODUCTOR,"B",2,1000);
            c.avanzarCiclo(); c.avanzarCiclo();
            long hasta = p.getContextoBuffer().getRedHasta();
            rechazar(() -> c.cambiarLatencia(-1));
            verificar(c.getConfiguracion().getLatencia() == 5, "La latencia invalida modifico la sesion");
            c.cambiarLatencia(0);
            verificar(p.getContextoBuffer().getRedHasta() == hasta, "Cambio la espera ya iniciada");
            for (int ciclo = 3; ciclo <= 17; ciclo++) c.avanzarCiclo();
            verificar(p.getEstado() == EstadoProceso.TERMINADO && p.getPc() == 12,
                    "No aplico cero a la segunda operacion");
            c.cambiarLatencia(4);
            Path archivo = Files.createTempFile("avilaos-remoto-", ".csv");
            try {
                ArchivoConfiguracion.guardar(archivo,c.getCargaInicial());
                c.cargarCarga(ArchivoConfiguracion.cargar(archivo));
                Proceso nuevo = c.getComputadores()[1].getListos()[0];
                verificar(c.getConfiguracion().getLatencia() == 4 && nuevo.getBuffer().getAnfitrion() == 1
                        && nuevo.getPc() == 0 && nuevo.getComputador() == 2, "No reconstruyo la carga remota");
            } finally { Files.deleteIfExists(archivo); }
        }
    }

    private static void comprobarVencimiento() {
        try (ControladorSimulacion c = crear(10)) {
            Buffer<Object> b = c.crearBuffer("B",1,1);
            Proceso p = crearProceso(c,"Vence en red",2,TipoProceso.PRODUCTOR,"B",1,3);
            for (int i = 0; i < 20; i++) c.avanzarCiclo();
            verificar(p.getEstado() == EstadoProceso.TERMINADO && p.getPc() == 1
                    && c.getComputadores()[1].getBloqueados().length == 0 && b.getVacios().getValor() == 1,
                    "Quedo un proceso vencido esperando red o se perdio un permiso");
        }
        try (ControladorSimulacion c = crear(0)) {
            Buffer<Object> b = c.crearBuffer("B",1,1);
            Proceso consumidor = crearProceso(c,"Vence al recibir",2,TipoProceso.CONSUMIDOR,"B",1,6);
            crearProceso(c,"Productor",1,TipoProceso.PRODUCTOR,"B",1,100);
            for (int i = 0; i < 10; i++) c.avanzarCiclo();
            verificar(consumidor.getEstado() == EstadoProceso.TERMINADO && consumidor.getPc() == 2
                    && b.getCantidad() == 1 && b.getLlenos().getValor() == 1
                    && b.getMutexSimulado().getValor() == 1, "Se perdio el permiso remoto del receptor vencido");
        }
    }

    private static void comprobarEjemplo() throws Exception {
        try (ControladorSimulacion c = crear(0)) {
            c.cargarCarga(ArchivoConfiguracion.cargar(Path.of("ejemplos/productor-consumidor-remoto.csv")));
            for (int ciclo = 0; ciclo < 200 && terminados(c) < 3; ciclo++) c.avanzarCiclo();
            verificar(terminados(c) == 3 && c.consultarBuffers()[0].cantidad() == 0
                    && c.getConfiguracion().getLatencia() == 3, "El ejemplo remoto no completo");
        }
    }

    private static void comprobarCruce(boolean automatico) throws Exception {
        try (ControladorSimulacion c = crear(3)) {
            Buffer<Object> b1 = c.crearBuffer("B1",2,1), b2 = c.crearBuffer("B2",2,2);
            Proceso[] procesos = {
                crearProceso(c,"P1",1,TipoProceso.PRODUCTOR,"B1",4,2000),
                crearProceso(c,"C1",1,TipoProceso.CONSUMIDOR,"B2",4,2000),
                crearProceso(c,"P2",2,TipoProceso.PRODUCTOR,"B2",4,2000),
                crearProceso(c,"C2",2,TipoProceso.CONSUMIDOR,"B1",4,2000)
            };
            if (automatico) {
                c.iniciar();
                long limite = System.nanoTime() + 5_000_000_000L;
                while (terminados(c) < 4 && System.nanoTime() < limite && !c.getEstado().startsWith("Error")) Thread.sleep(2);
                c.detener();
            } else {
                for (int ciclo = 1; ciclo <= 300 && terminados(c) < 4; ciclo++) {
                    c.avanzarCiclo();
                    comprobarInvariantes(b1,procesos);
                    comprobarInvariantes(b2,procesos);
                }
            }
            verificar(terminados(c) == 4 && b1.getCantidad() == 0 && b2.getCantidad() == 0,
                    "No completo el intercambio cruzado entre computadores");
            for (Proceso p : procesos) verificar(p.getContextoBuffer().getCompletados() == 4,
                    "Repitio o perdio operaciones remotas");
            comprobarInvariantes(b1,procesos);
            comprobarInvariantes(b2,procesos);
        }
    }

    private static int terminados(ControladorSimulacion c) {
        int total = 0;
        for (Computador equipo : c.getComputadores()) total += equipo.getTerminados().length;
        return total;
    }

    private static void comprobarInvariantes(Buffer<?> b, Proceso[] procesos) {
        int espacios = 0, elementos = 0, mutex = 0;
        for (Proceso p : procesos) {
            if (p.getBuffer() != b) continue;
            ContextoBuffer contexto = p.getContextoBuffer();
            if (contexto.tieneMutex()) mutex++;
            if (!contexto.tieneRecurso()) continue;
            if ((p.getTipo() == TipoProceso.PRODUCTOR) != contexto.estaModificado()) espacios++;
            else elementos++;
        }
        verificar(b.getVacios().getValor() + espacios + b.getCantidad() == b.getCapacidad(), "Espacios remotos perdidos");
        verificar(b.getLlenos().getValor() + elementos == b.getCantidad(), "Elementos remotos perdidos");
        verificar(b.getMutexSimulado().getValor() + mutex == 1, "Mutex remoto perdido o duplicado");
    }
}
