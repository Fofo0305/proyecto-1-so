package Pruebas;

import Modelo.DefinicionProceso;
import Modelo.EstadoProceso;
import Modelo.EventoSistema;
import Modelo.ModoUsuario;
import Modelo.MotivoBloqueo;
import Modelo.OperacionES;
import Modelo.ParametrosArchivo;
import Modelo.PermisoArchivo;
import Modelo.Proceso;
import Modelo.VistaSolicitudDisco;
import Sistema.ComponenteReloj;
import Sistema.Computador;
import Sistema.Disco;
import Sistema.RelojGlobal;
import Sistema.SistemaArchivos;

/**
 * Procesos que hacen CRUD a traves del disco: piden en modo sistema, esperan bloqueados por
 * DISCO, el sistema de archivos cambia cuando el disco termina y los rechazos no detienen la
 * simulacion. @author Luciano
 */
public final class PruebaProcesosES {
    private PruebaProcesosES() { }

    /** Un computador con disco y un arbol pequeno: /home/ana (de ana) y /docs (publico). */
    private static final class Entorno implements AutoCloseable {
        final Disco disco = new Disco(100, 20, 0);
        final SistemaArchivos sistema = new SistemaArchivos(disco);
        final Computador computador = new Computador(1, 100);
        final RelojGlobal reloj;

        Entorno() {
            sistema.crearDirectorio("/home", "admin", PermisoArchivo.PUBLICO);
            sistema.crearDirectorio("/home/ana", "ana", PermisoArchivo.PRIVADO);
            sistema.crearArchivo("/home/ana/notas.txt", "ana", 3, PermisoArchivo.PRIVADO, 0);
            sistema.crearDirectorio("/docs", "admin", PermisoArchivo.PUBLICO);
            sistema.crearArchivo("/docs/guia.txt", "admin", 2, PermisoArchivo.PUBLICO, 0);
            computador.conectarAlmacenamiento(disco, sistema);
            reloj = new RelojGlobal(new ComponenteReloj[] {computador, disco});
        }

        Proceso lanzar(String nombre, OperacionES operacion, String ruta, ParametrosArchivo datos) {
            Proceso proceso = new DefinicionProceso(nombre, 1, 1, 4, 3, 500, operacion, ruta, datos).crearProceso();
            computador.recibirProceso(proceso);
            computador.admitirProcesos();
            return proceso;
        }

        void correr(int ciclos) { for (int i = 0; i < ciclos; i++) reloj.avanzarCiclo(); }

        @Override public void close() { reloj.close(); }
    }

    private static ParametrosArchivo como(String usuario, int tamano, String nuevoNombre) {
        return new ParametrosArchivo(tamano, PermisoArchivo.PRIVADO, nuevoNombre,
                usuario.equals("admin") ? ModoUsuario.ADMINISTRADOR : ModoUsuario.USUARIO, usuario);
    }

    public static void main(String[] args) {
        filtroDeLlegada();
        crearEsperaYCompleta();
        rechazoNoDetiene();
        actualizarYEliminar();
        // Computador y disco corren en hilos distintos: se repite para que una carrera se note.
        String primera = huella();
        for (int corrida = 0; corrida < 20; corrida++)
            verificar(primera.equals(huella()), "Dos corridas iguales no producen el mismo log");
        System.out.println("PruebaProcesosES OK");
    }

    /** El disco no atiende en el mismo ciclo en que llega la solicitud, ni la entrega antes de tiempo. */
    private static void filtroDeLlegada() {
        Disco disco = new Disco(100, 20, 10);
        disco.encolarSolicitud(5, OperacionES.LEER, 10, 1, 1);
        disco.avanzarCiclo(1);
        verificar(disco.consultarActiva() == null && disco.consultarCompletadas().length == 0,
                "El disco atendio una solicitud en su ciclo de llegada");
        disco.avanzarCiclo(2);
        verificar(disco.consultarCompletadas().length == 1, "El disco no atendio la solicitud en el ciclo siguiente");
        verificar(disco.tomarCompletadas(2).length == 0, "Se entrego una solicitud en el mismo ciclo en que termino");
        VistaSolicitudDisco[] listas = disco.tomarCompletadas(3);
        verificar(listas.length == 1 && listas[0].procesoId() == 5, "No se entrego la solicitud terminada");
        verificar(disco.tomarCompletadas(10).length == 0, "Una solicitud se entrego dos veces");
        try {
            disco.encolarSolicitud(5, OperacionES.LEER, 10, 1, 1);
            throw new AssertionError("El disco acepto una solicitud de un ciclo pasado");
        } catch (IllegalArgumentException esperado) { }
    }

    private static void crearEsperaYCompleta() {
        try (Entorno entorno = new Entorno()) {
            Proceso creador = entorno.lanzar("crear", OperacionES.CREAR, "/home/ana/nuevo.txt", como("ana", 2, null));
            entorno.correr(1);
            verificar(creador.getEstado() == EstadoProceso.BLOQUEADO
                    && creador.getMotivoBloqueo() == MotivoBloqueo.DISCO && creador.getSolicitudDisco() > 0,
                    "El proceso no queda bloqueado esperando al disco");
            verificar(creador.getPc() == 1, "La solicitud no cuenta como una instruccion");
            verificar(!entorno.sistema.existe("/home/ana/nuevo.txt"), "El archivo aparecio antes de que el disco terminara");
            entorno.correr(30);
            verificar(entorno.sistema.existe("/home/ana/nuevo.txt"), "El disco termino y el archivo no se creo");
            verificar(creador.getEstado() == EstadoProceso.TERMINADO && "COMPLETADO".equals(creador.getResultado()),
                    "El proceso no termino sus instrucciones despues de la E/S");
            verificar(contiene(entorno.computador.consultarEventos(), "FIN_ARCHIVO", "creo /home/ana/nuevo.txt"),
                    "El log no registra el fin de la operacion");
        }
    }

    private static void rechazoNoDetiene() {
        try (Entorno entorno = new Entorno()) {
            Proceso intruso = entorno.lanzar("espiar", OperacionES.LEER, "/home/ana/notas.txt", como("luis", 1, null));
            Proceso fuera = entorno.lanzar("fuera", OperacionES.CREAR, "/docs/mio.txt", como("luis", 1, null));
            Proceso legitimo = entorno.lanzar("leer", OperacionES.LEER, "/docs/guia.txt", como("luis", 1, null));
            entorno.correr(40);
            verificar(contiene(entorno.computador.consultarEventos(), "RECHAZO_ARCHIVO", "espiar") || contiene(
                    entorno.computador.consultarEventos(), "RECHAZO_ARCHIVO", "LEER /home/ana/notas.txt"),
                    "No se explica el rechazo de la lectura privada");
            verificar(contiene(entorno.computador.consultarEventos(), "RECHAZO_ARCHIVO", "CREAR /docs/mio.txt"),
                    "Un usuario creo un archivo fuera de sus directorios");
            verificar(!entorno.sistema.existe("/docs/mio.txt"), "El archivo rechazado existe");
            for (Proceso proceso : new Proceso[] {intruso, fuera, legitimo})
                verificar(proceso.getEstado() == EstadoProceso.TERMINADO && "COMPLETADO".equals(proceso.getResultado()),
                        "El rechazo detuvo al proceso " + proceso.getNombre());
            verificar(contiene(entorno.computador.consultarEventos(), "FIN_ARCHIVO", "leyo /docs/guia.txt"),
                    "La lectura publica no se atendio");
        }
    }

    private static void actualizarYEliminar() {
        try (Entorno entorno = new Entorno()) {
            entorno.lanzar("reescribir", OperacionES.ACTUALIZAR, "/home/ana/notas.txt", como("ana", 1, null));
            entorno.lanzar("renombrar", OperacionES.ACTUALIZAR, "/docs/guia.txt", como("admin", 1, "manual.txt"));
            entorno.lanzar("renombrar-usuario", OperacionES.ACTUALIZAR, "/home/ana/notas.txt", como("ana", 1, "x.txt"));
            entorno.correr(40);
            EventoSistema[] eventos = entorno.computador.consultarEventos();
            verificar(contiene(eventos, "FIN_ARCHIVO", "reescribio /home/ana/notas.txt"), "No se reescribio el archivo");
            verificar(entorno.sistema.existe("/docs/manual.txt") && !entorno.sistema.existe("/docs/guia.txt"),
                    "El administrador no pudo renombrar");
            verificar(contiene(eventos, "RECHAZO_ARCHIVO", "ACTUALIZAR /home/ana/notas.txt como ana"),
                    "Un usuario pudo renombrar");

            int libresAntes = entorno.disco.contarLibres(Modelo.AreaDisco.DATOS);
            entorno.lanzar("borrar", OperacionES.ELIMINAR, "/home", como("admin", 1, null));
            entorno.correr(40);
            verificar(!entorno.sistema.existe("/home") && !entorno.sistema.existe("/home/ana/notas.txt"),
                    "Eliminar un directorio no elimino su subarbol");
            verificar(entorno.disco.contarLibres(Modelo.AreaDisco.DATOS) == libresAntes + 3,
                    "Eliminar no libero los bloques del subarbol");
        }
    }

    /** Log de computador y disco de una corrida con varios procesos compitiendo por el disco. */
    private static String huella() {
        try (Entorno entorno = new Entorno()) {
            entorno.lanzar("a", OperacionES.CREAR, "/home/ana/a.txt", como("ana", 4, null));
            entorno.lanzar("b", OperacionES.LEER, "/docs/guia.txt", como("luis", 1, null));
            entorno.lanzar("c", OperacionES.LEER, "/home/ana/notas.txt", como("ana", 1, null));
            entorno.lanzar("d", OperacionES.ELIMINAR, "/docs", como("admin", 1, null));
            entorno.correr(60);
            StringBuilder texto = new StringBuilder();
            for (EventoSistema evento : entorno.computador.consultarEventos())
                texto.append(evento.ciclo()).append(evento.tipo()).append(evento.detalle()).append('\n');
            for (EventoSistema evento : entorno.disco.consultarEventos())
                texto.append(evento.ciclo()).append(evento.tipo()).append(evento.detalle()).append('\n');
            return texto.toString();
        }
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
