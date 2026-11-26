package Sistema;

import EDD.Cola;
import Modelo.CargaInicial;
import Modelo.Configuracion;
import Modelo.DefinicionProceso;
import Modelo.EventoSistema;
import Modelo.ModoUsuario;
import Modelo.OperacionES;
import Modelo.ParametrosArchivo;
import Modelo.ParametrosES;
import Modelo.PermisoArchivo;
import Modelo.PoliticaDisco;
import Modelo.PoliticaPlanificacion;
import Modelo.Proceso;
import Modelo.TipoProceso;
import Modelo.VistaAlmacenamiento;
import Modelo.VistaComputador;
import Modelo.VistaMetricas;
import Persistencia.ArchivoProcesos;
import Persistencia.ArchivoSistemaArchivos;
import java.util.concurrent.Semaphore;

/** Controla la única instancia del computador sin depender de Swing. @author Adolfo */
public class ControladorSimulacion implements AutoCloseable {
    /** Disco por defecto cuando no se carga el JSON del sistema de archivos. */
    public static final int BLOQUES_POR_DEFECTO = Configuracion.BLOQUES_DISCO_POR_DEFECTO;
    public static final int SWAP_POR_DEFECTO = Configuracion.SWAP_POR_DEFECTO;
    private final Semaphore mutex = new Semaphore(1, true);
    private String jsonSistemaArchivos;
    private Disco disco;
    private SistemaArchivos sistemaArchivos;
    private Configuracion configuracion;
    private Computador computador;
    private RelojGlobal reloj;
    private boolean cerrado;
    private boolean pausado;
    private boolean detenido;
    private Cola<DefinicionProceso> cargaProcesos = new Cola<>();
    /** Si los procesos actuales vienen del JSON de procesos (y no del formulario o un CSV). */
    private boolean procesosJson;

    public void configurar(Configuracion nueva) {
        cargarCarga(new CargaInicial(nueva, new DefinicionProceso[0]));
    }

    public void cargarCarga(CargaInicial carga) {
        if (carga == null) throw new IllegalArgumentException("La carga es obligatoria");
        mutex.acquireUninterruptibly();
        try {
            comprobarModificable("cambiar la configuracion");
            reconstruir(carga);
            procesosJson = false;
        } finally { mutex.release(); }
    }

    /**
     * Carga el JSON de procesos: configuracion del computador, swap y politica de disco inicial,
     * y los procesos. El disco que ya estaba cargado se conserva (con lo que hayan cambiado los
     * procesos anteriores); solo el swap pasa a ser el del JSON, recortado si no cabe.
     */
    public void cargarProcesos(String json) {
        CargaInicial leida = ArchivoProcesos.leer(json);
        mutex.acquireUninterruptibly();
        try {
            comprobarModificable("cargar otros procesos");
            Configuracion nueva = leida.getConfiguracion();
            if (configuracion != null) {
                // El disco ya cargado manda en bloques y cabezal; el swap pedido se acota a lo que cabe.
                int limite = (disco != null ? disco.getTotalBloques() : configuracion.getBloquesDisco()) - 1;
                nueva = nueva.conDisco(configuracion.getBloquesDisco(), Math.min(nueva.getBloquesSwap(),
                        Math.min(limite, configuracion.getBloquesDisco() - 1)), nueva.getPoliticaDisco(),
                        configuracion.getCabezalInicial());
            }
            String anterior = jsonSistemaArchivos;
            if (jsonSistemaArchivos != null && disco != null)
                jsonSistemaArchivos = ArchivoSistemaArchivos.escribir(
                        new ArchivoSistemaArchivos.SistemaCargado(disco, sistemaArchivos), false);
            try {
                reconstruir(new CargaInicial(nueva, leida.getProcesos()));
            } catch (RuntimeException error) {
                jsonSistemaArchivos = anterior;
                throw error;
            }
            procesosJson = true;
        } finally { mutex.release(); }
    }

    /**
     * Que falta para iniciar como pide el enunciado (los dos JSON cargados); null si no falta nada.
     * {@link #iniciar()} no lo exige, para poder probar el simulador con procesos del formulario.
     */
    public String faltaParaIniciar() {
        mutex.acquireUninterruptibly();
        try {
            if (cerrado || reloj == null) return "Configure primero la simulacion";
            boolean sinDisco = jsonSistemaArchivos == null;
            if (sinDisco && !procesosJson) return "Faltan el JSON del disco y el JSON de procesos";
            if (sinDisco) return "Falta cargar el JSON del disco";
            if (!procesosJson) return "Falta cargar el JSON de procesos";
            return null;
        } finally { mutex.release(); }
    }

    /** JSON del disco y el arbol tal como estan ahora; null si no hay simulacion. */
    public String exportarSistemaArchivos() {
        mutex.acquireUninterruptibly();
        try {
            if (disco == null) return null;
            return ArchivoSistemaArchivos.escribir(new ArchivoSistemaArchivos.SistemaCargado(disco, sistemaArchivos));
        } finally { mutex.release(); }
    }

    private void comprobarModificable(String accion) {
        if (cerrado) throw new IllegalStateException("El controlador esta cerrado");
        if (reloj != null && reloj.estaActivo())
            throw new IllegalStateException("Detenga la simulacion antes de " + accion);
    }

    /** Arma de nuevo computador, disco y reloj con la carga; se llama con el mutex tomado. */
    private void reconstruir(CargaInicial carga) {
        Configuracion nueva = carga.getConfiguracion();
        Computador nuevo = new Computador(1, nueva.getRam()[0]);
        nuevo.cambiarPolitica(nueva.getPoliticas()[0], nueva.getQuantums()[0]);
        Cola<DefinicionProceso> definiciones = new Cola<>();
        for (DefinicionProceso definicion : carga.getProcesos()) {
            nuevo.recibirProceso(definicion.crearProceso());
            definiciones.agregar(definicion);
        }
        nuevo.admitirProcesos();
        ArchivoSistemaArchivos.SistemaCargado almacenamiento = crearAlmacenamiento(jsonSistemaArchivos, nueva);
        nuevo.conectarAlmacenamiento(almacenamiento.disco(), almacenamiento.sistema());
        RelojGlobal nuevoReloj = new RelojGlobal(new ComponenteReloj[] {nuevo, almacenamiento.disco()});
        nuevoReloj.setDuracionMs(nueva.getDuracionMs());
        if (reloj != null) reloj.close();
        configuracion = nueva;
        computador = nuevo;
        disco = almacenamiento.disco();
        sistemaArchivos = almacenamiento.sistema();
        reloj = nuevoReloj;
        pausado = false;
        detenido = false;
        cargaProcesos = definiciones;
    }

    /**
     * Disco del JSON (sus bloques y cabezal mandan) o uno vacio con los datos de la configuracion.
     * El swap y la politica inicial salen siempre de la configuracion.
     */
    private static ArchivoSistemaArchivos.SistemaCargado crearAlmacenamiento(String json, Configuracion conf) {
        ArchivoSistemaArchivos.SistemaCargado cargado;
        if (json != null) {
            cargado = ArchivoSistemaArchivos.leer(json, conf.getBloquesSwap());
        } else {
            Disco vacio = new Disco(conf.getBloquesDisco(), conf.getBloquesSwap(), conf.getCabezalInicial());
            cargado = new ArchivoSistemaArchivos.SistemaCargado(vacio, new SistemaArchivos(vacio));
        }
        if (cargado.disco().getPolitica() != conf.getPoliticaDisco())
            cargado.disco().cambiarPolitica(planificador(conf.getPoliticaDisco()));
        return cargado;
    }

    private static PlanificacionDisco.PoliticaPlanificacionDisco planificador(PoliticaDisco tipo) {
        return switch (tipo) {
            case FIFO -> new PlanificacionDisco.FIFO();
            case SSTF -> new PlanificacionDisco.SSTF();
            case SCAN -> new PlanificacionDisco.SCAN();
            case C_SCAN -> new PlanificacionDisco.CSCAN();
        };
    }

    /**
     * Carga el JSON del sistema de archivos. Se valida al momento: si esta mal formado no
     * reemplaza al anterior. Si ya hay una simulacion, se arma de nuevo con este disco y los
     * mismos procesos. {@code null} vuelve al disco vacio.
     */
    public void cargarSistemaArchivos(String json) {
        if (json != null) ArchivoSistemaArchivos.leer(json, SWAP_POR_DEFECTO);
        mutex.acquireUninterruptibly();
        try {
            comprobarModificable("cambiar el sistema de archivos");
            String anterior = jsonSistemaArchivos;
            jsonSistemaArchivos = json;
            if (reloj == null) return;
            try {
                reconstruir(new CargaInicial(configuracion, definicionesActuales()));
            } catch (RuntimeException error) {
                jsonSistemaArchivos = anterior;
                throw error;
            }
        } finally { mutex.release(); }
    }

    private DefinicionProceso[] definicionesActuales() {
        Object[] elementos = cargaProcesos.instantanea();
        DefinicionProceso[] definiciones = new DefinicionProceso[elementos.length];
        for (int i = 0; i < elementos.length; i++) definiciones[i] = (DefinicionProceso) elementos[i];
        return definiciones;
    }

    /**
     * Crea un proceso que hace una operacion CRUD con la identidad de la sesion actual. Los
     * permisos se revisan de una vez para avisar en la interfaz; el resto (por ejemplo, si el
     * archivo existe) se decide cuando el proceso llega a la CPU, porque puede cambiar antes.
     * @throws SecurityException si la sesion no puede hacer esa operacion
     */
    public Proceso crearProcesoArchivo(String nombre, int prioridad, int memoria, int instrucciones,
            int deadline, OperacionES operacion, String ruta, int tamanoBloques,
            PermisoArchivo permiso, String nuevoNombre) {
        ParametrosArchivo datos;
        mutex.acquireUninterruptibly();
        try {
            comprobarDisponible();
            datos = new ParametrosArchivo(tamanoBloques, permiso, nuevoNombre,
                    sistemaArchivos.getModo(), sistemaArchivos.getUsuario());
            try {
                sistemaArchivos.planificar(operacion, ruta, datos);
            } catch (IllegalArgumentException | IllegalStateException pendiente) {
                // No es de permisos: se vuelve a revisar cuando el proceso pida el disco.
            }
        } finally { mutex.release(); }
        return crear(new DefinicionProceso(nombre, 1, prioridad, memoria, instrucciones, deadline,
                operacion, ruta, datos));
    }

    /** Cambia quien usa el simulador; los procesos que se creen despues usan esa identidad. */
    public void cambiarSesion(ModoUsuario modo, String usuario) {
        mutex.acquireUninterruptibly();
        try { comprobarDisponible(); sistemaArchivos.cambiarSesion(modo, usuario); }
        finally { mutex.release(); }
    }

    public Proceso crearProceso(String nombre, int equipo, int prioridad,
            int memoria, int instrucciones, int deadline) {
        return crear(new DefinicionProceso(nombre, equipo, prioridad, memoria, instrucciones, deadline));
    }

    public Proceso crearProcesoES(String nombre, int equipo, int prioridad, int memoria,
            int instrucciones, int deadline, int rafaga, int duracion) {
        return crear(new DefinicionProceso(nombre, equipo, prioridad, memoria, instrucciones,
                deadline, TipoProceso.IO_BOUND, new ParametrosES(rafaga, duracion)));
    }

    private Proceso crear(DefinicionProceso definicion) {
        mutex.acquireUninterruptibly();
        try {
            comprobarDisponible();
            if (definicion.memoria() > computador.getMemoria().getCapacidad())
                throw new IllegalArgumentException("La memoria del proceso supera la RAM total");
            Proceso proceso = definicion.crearProceso();
            computador.recibirProceso(proceso);
            computador.admitirProcesos();
            cargaProcesos.agregar(definicion);
            return proceso;
        } finally { mutex.release(); }
    }

    public void cambiarPolitica(int equipo, PoliticaPlanificacion tipo, int quantum) {
        if (equipo != 1) throw new IllegalArgumentException("El unico computador tiene ID 1");
        mutex.acquireUninterruptibly();
        try {
            comprobarDisponible();
            computador.cambiarPolitica(tipo, quantum);
            configuracion = new Configuracion(configuracion.getRam(), configuracion.getDuracionMs(),
                    quantum, new PoliticaPlanificacion[] {tipo}, new int[] {quantum}).conDiscoDe(configuracion);
        } finally { mutex.release(); }
    }

    public boolean cancelarProceso(Proceso proceso) {
        mutex.acquireUninterruptibly();
        try { comprobarDisponible(); return computador.terminarProceso(proceso); }
        finally { mutex.release(); }
    }

    /** Cambia la politica con que el disco elige la proxima solicitud; vale desde el siguiente ciclo. */
    public void cambiarPoliticaDisco(PoliticaDisco tipo) {
        if (tipo == null) throw new IllegalArgumentException("La politica de disco es obligatoria");
        mutex.acquireUninterruptibly();
        try {
            comprobarDisponible();
            disco.cambiarPolitica(planificador(tipo));
        } finally { mutex.release(); }
    }

    /** Cuantos rechazos recientes guarda la vista del almacenamiento. */
    private static final int RECHAZOS_VISIBLES = 8;

    /** Disco, sistema de archivos y sesion en una sola lectura; null si no hay simulacion. */
    public VistaAlmacenamiento consultarAlmacenamiento() {
        mutex.acquireUninterruptibly();
        try {
            if (computador == null || disco == null) return null;
            EventoSistema[] eventos = computador.consultarEventos();
            Cola<EventoSistema> recientes = new Cola<>();
            for (EventoSistema evento : eventos) {
                if (!evento.tipo().equals("RECHAZO_ARCHIVO")) continue;
                recientes.agregar(evento);
                if (recientes.getTamano() > RECHAZOS_VISIBLES) recientes.quitar();
            }
            EventoSistema[] rechazos = new EventoSistema[recientes.getTamano()];
            for (int i = 0; i < rechazos.length; i++) rechazos[i] = recientes.quitar();
            return new VistaAlmacenamiento(reloj.getCiclo(), disco.getPolitica(), disco.getPosicionCabezal(),
                    disco.getDireccion(), disco.getTamanoSwap(), disco.consultarBloques(), disco.consultarActiva(),
                    disco.consultarCola(), disco.consultarMetricas(), disco.consultarEventos(),
                    sistemaArchivos.consultarArbol(), sistemaArchivos.consultarAsignacion(),
                    sistemaArchivos.getModo(), sistemaArchivos.getUsuario(), rechazos);
        } finally { mutex.release(); }
    }

    public CargaInicial getCargaInicial() {
        mutex.acquireUninterruptibly();
        try {
            if (configuracion == null) throw new IllegalStateException("Configure primero la simulacion");
            return new CargaInicial(configuracion, definicionesActuales());
        } finally { mutex.release(); }
    }

    public VistaMetricas[] consultarMetricas() {
        mutex.acquireUninterruptibly();
        try { return computador == null ? new VistaMetricas[0] : new VistaMetricas[] {computador.consultarMetricas()}; }
        finally { mutex.release(); }
    }

    public EventoSistema[] consultarLog() {
        mutex.acquireUninterruptibly();
        try { return computador == null ? new EventoSistema[0] : computador.consultarEventos(); }
        finally { mutex.release(); }
    }

    public void iniciar() { mutex.acquireUninterruptibly(); try { comprobarDisponible(); reloj.iniciar(); pausado = false; } finally { mutex.release(); } }
    public void avanzarCiclo() { mutex.acquireUninterruptibly(); try { comprobarDisponible(); reloj.avanzarCiclo(); } finally { mutex.release(); } }
    public void pausar() { mutex.acquireUninterruptibly(); try { comprobarDisponible(); reloj.pausar(); pausado = true; } finally { mutex.release(); } }
    public void reanudar() { mutex.acquireUninterruptibly(); try { comprobarDisponible(); reloj.reanudar(); pausado = false; } finally { mutex.release(); } }

    public void detener() {
        mutex.acquireUninterruptibly();
        try {
            if (cerrado || reloj == null) throw new IllegalStateException("Configure primero la simulacion");
            reloj.detener();
            detenido = true;
            pausado = false;
        } finally { mutex.release(); }
    }

    public void cambiarDuracion(int duracionMs) {
        mutex.acquireUninterruptibly();
        try {
            comprobarDisponible();
            configuracion = new Configuracion(configuracion.getRam(), duracionMs, configuracion.getQuantum(),
                    configuracion.getPoliticas(), configuracion.getQuantums()).conDiscoDe(configuracion);
            reloj.setDuracionMs(duracionMs);
        } finally { mutex.release(); }
    }

    private void comprobarDisponible() {
        if (cerrado || reloj == null || detenido) throw new IllegalStateException("Configure una simulacion disponible");
        if (reloj.getError() != null) throw new IllegalStateException("La simulacion fallo", reloj.getError());
    }

    public String getEstado() {
        mutex.acquireUninterruptibly();
        try {
            if (cerrado) return "Cerrado";
            if (reloj == null) return "Sin configurar";
            if (reloj.getError() != null) return "Error: " + reloj.getError().getMessage();
            if (detenido) return "Detenido";
            if (pausado) return "Pausado";
            return reloj.estaActivo() ? "En ejecucion" : "Preparado";
        } finally { mutex.release(); }
    }

    public Configuracion getConfiguracion() { mutex.acquireUninterruptibly(); try { return configuracion; } finally { mutex.release(); } }
    public Computador[] getComputadores() { mutex.acquireUninterruptibly(); try { return computador == null ? new Computador[0] : new Computador[] {computador}; } finally { mutex.release(); } }
    public VistaComputador[] consultarVistas() { mutex.acquireUninterruptibly(); try { return computador == null ? new VistaComputador[0] : new VistaComputador[] {computador.consultarVista()}; } finally { mutex.release(); } }
    public Disco getDisco() { mutex.acquireUninterruptibly(); try { return disco; } finally { mutex.release(); } }
    public SistemaArchivos getSistemaArchivos() { mutex.acquireUninterruptibly(); try { return sistemaArchivos; } finally { mutex.release(); } }
    public long getCiclo() { mutex.acquireUninterruptibly(); try { return reloj == null ? 0 : reloj.getCiclo(); } finally { mutex.release(); } }

    @Override public void close() {
        mutex.acquireUninterruptibly();
        try { cerrado = true; if (reloj != null) reloj.close(); }
        finally { mutex.release(); }
    }
}
