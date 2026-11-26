package Pruebas;

import Modelo.AreaDisco;
import Modelo.DefinicionProceso;
import Modelo.EstadoProceso;
import Modelo.EventoSistema;
import Modelo.ParametrosES;
import Modelo.Proceso;
import Modelo.TipoProceso;
import Modelo.VistaBloqueDisco;
import Modelo.VistaMetricasDisco;
import Sistema.ComponenteReloj;
import Sistema.Computador;
import Sistema.Disco;
import Sistema.RelojGlobal;
import Sistema.SistemaArchivos;

/**
 * Planificador a mediano plazo: cuando un proceso nuevo no cabe en la RAM se suspende al que
 * lleva mas tiempo bloqueado, su imagen va al swap con una solicitud de disco, pasa a
 * listo/suspendido cuando ocurre su evento y vuelve a la RAM cuando hay espacio. @author Luciano
 */
public final class PruebaSwap {
    private PruebaSwap() { }

    /** RAM de 6 unidades y un swap de 20 bloques (80-99), con el cabezal cerca del swap. */
    private static final class Entorno implements AutoCloseable {
        final Disco disco = new Disco(100, 20, 75);
        final Computador computador = new Computador(1, 6);
        final RelojGlobal reloj;

        Entorno() {
            computador.conectarAlmacenamiento(disco, new SistemaArchivos(disco));
            reloj = new RelojGlobal(new ComponenteReloj[] {computador, disco});
        }

        Proceso lanzar(String nombre, int memoria, int instrucciones, TipoProceso tipo, ParametrosES es) {
            Proceso proceso = new DefinicionProceso(nombre, 1, 1, memoria, instrucciones, 500, tipo, es).crearProceso();
            computador.recibirProceso(proceso);
            computador.admitirProcesos();
            return proceso;
        }

        /** Dos procesos de E/S que llenan la RAM y un tercero que ya no cabe. */
        Proceso[] escenario() {
            Proceso a = lanzar("a", 3, 4, TipoProceso.IO_BOUND, new ParametrosES(1, 12));
            Proceso b = lanzar("b", 3, 4, TipoProceso.IO_BOUND, new ParametrosES(1, 40));
            Proceso c = lanzar("c", 3, 3, TipoProceso.CPU_BOUND, null);
            return new Proceso[] {a, b, c};
        }

        void correr(int ciclos) { for (int i = 0; i < ciclos; i++) reloj.avanzarCiclo(); }

        void correrHasta(java.util.function.BooleanSupplier condicion, int limite, String mensaje) {
            for (int i = 0; i < limite && !condicion.getAsBoolean(); i++) reloj.avanzarCiclo();
            verificar(condicion.getAsBoolean(), mensaje);
        }

        @Override public void close() { reloj.close(); }
    }

    public static void main(String[] args) {
        recorridoCompleto();
        terminarEnSwapLibera();
        String primera = huella();
        for (int corrida = 0; corrida < 20; corrida++)
            verificar(primera.equals(huella()), "Dos corridas iguales no producen el mismo log");
        System.out.println("PruebaSwap OK");
    }

    private static void recorridoCompleto() {
        try (Entorno entorno = new Entorno()) {
            Proceso[] p = entorno.escenario();
            Proceso a = p[0], c = p[2];
            verificar(c.getEstado() == EstadoProceso.NUEVO, "El tercer proceso cupo en una RAM llena");
            entorno.correrHasta(() -> a.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO, 10,
                    "No se suspendio al proceso que lleva mas tiempo bloqueado");
            verificar(p[1].getEstado() != EstadoProceso.BLOQUEADO_SUSPENDIDO, "Se suspendio a mas procesos de los necesarios");
            verificar(c.getEstado() != EstadoProceso.NUEVO, "El proceso nuevo no entro despues de la suspension");
            verificar(contiene(entorno.computador.consultarEventos(), "SUSPENSION",
                    "Se suspende al proceso P" + a.getId() + " para admitir a P" + c.getId()),
                    "El log no explica a quien se suspende y para quien");
            verificar(bloquesDe(entorno.disco, "P" + a.getId(), AreaDisco.SWAP) == a.getMemoria(),
                    "La imagen del proceso no ocupa bloques del area de intercambio");

            entorno.correrHasta(() -> a.getEstado() == EstadoProceso.LISTO_SUSPENDIDO
                    || a.getEstado() == EstadoProceso.LISTO, 30, "El fin de su E/S no lo paso a listo/suspendido");
            verificar(contiene(entorno.computador.consultarEventos(), "FIN_ES", "pasa a listo/suspendido"),
                    "El log no registra el paso a listo/suspendido");
            entorno.correrHasta(() -> contiene(entorno.computador.consultarEventos(), "FIN_REACTIVACION", ""), 40,
                    "El proceso no volvio a la RAM cuando hubo espacio");
            verificar(bloquesDe(entorno.disco, "P" + a.getId(), AreaDisco.SWAP) == 0,
                    "La reactivacion no libero el swap");
            entorno.correrHasta(() -> a.getEstado() == EstadoProceso.TERMINADO, 80, "El proceso reactivado no termino");
            verificar("COMPLETADO".equals(a.getResultado()), "El proceso reactivado no completo sus instrucciones");
            VistaMetricasDisco metricas = entorno.disco.consultarMetricas();
            verificar(metricas.suspensiones() == 1 && metricas.reactivaciones() == 1,
                    "El disco no conto la suspension y la reactivacion");
        }
    }

    /** Un proceso que termina estando en swap devuelve sus bloques. */
    private static void terminarEnSwapLibera() {
        try (Entorno entorno = new Entorno()) {
            Proceso a = entorno.escenario()[0];
            entorno.correrHasta(() -> a.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO, 10, "No hubo suspension");
            int libres = entorno.disco.contarLibres(AreaDisco.SWAP);
            verificar(entorno.computador.terminarProceso(a), "No se pudo terminar un proceso suspendido");
            verificar(entorno.disco.contarLibres(AreaDisco.SWAP) == libres + a.getMemoria(),
                    "Terminar un proceso suspendido no libero su swap");
            entorno.correr(20);
            verificar(entorno.computador.consultarVista().colaBloqueadosSuspendidos().length == 0
                    && entorno.computador.consultarVista().colaListosSuspendidos().length == 0,
                    "Un proceso terminado sigue en una cola de suspendidos");
        }
    }

    private static String huella() {
        try (Entorno entorno = new Entorno()) {
            long base = entorno.escenario()[0].getId();
            entorno.lanzar("d", 2, 3, TipoProceso.IO_BOUND, new ParametrosES(1, 5));
            entorno.correr(120);
            StringBuilder texto = new StringBuilder();
            for (EventoSistema evento : entorno.computador.consultarEventos())
                texto.append(evento.ciclo()).append(evento.tipo()).append(evento.detalle()).append('\n');
            for (EventoSistema evento : entorno.disco.consultarEventos())
                texto.append(evento.ciclo()).append(evento.tipo()).append(evento.detalle()).append('\n');
            // Los id de proceso son globales: se cuentan desde el primero de la corrida.
            return java.util.regex.Pattern.compile("P(\\d+)").matcher(texto)
                    .replaceAll(m -> "P" + (Long.parseLong(m.group(1)) - base));
        }
    }

    private static int bloquesDe(Disco disco, String propietario, AreaDisco area) {
        int cantidad = 0;
        for (VistaBloqueDisco bloque : disco.consultarBloques())
            if (bloque.area() == area && propietario.equals(bloque.propietario())) cantidad++;
        return cantidad;
    }

    private static boolean contiene(EventoSistema[] eventos, String tipo, String fragmento) {
        for (EventoSistema evento : eventos)
            if (evento.tipo().equals(tipo) && evento.detalle().contains(fragmento)) return true;
        return false;
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
