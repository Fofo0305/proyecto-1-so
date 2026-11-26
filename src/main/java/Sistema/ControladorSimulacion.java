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
import Modelo.PoliticaPlanificacion;
import Modelo.Proceso;
import Modelo.TipoProceso;
import Modelo.VistaComputador;
import Modelo.VistaMetricas;
import Persistencia.ArchivoSistemaArchivos;
import java.util.concurrent.Semaphore;

/** Controla la única instancia del computador sin depender de Swing. @author Adolfo */
public class ControladorSimulacion implements AutoCloseable {
    /** Disco por defecto cuando no se carga el JSON del sistema de archivos. */
    public static final int BLOQUES_POR_DEFECTO = 200;
    public static final int SWAP_POR_DEFECTO = 40;
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

    public void configurar(Configuracion nueva) {
        cargarCarga(new CargaInicial(nueva, new DefinicionProceso[0]));
    }

    public void cargarCarga(CargaInicial carga) {
        if (carga == null) throw new IllegalArgumentException("La carga es obligatoria");
        mutex.acquireUninterruptibly();
        try {
            if (cerrado) throw new IllegalStateException("El controlador esta cerrado");
            if (reloj != null && reloj.estaActivo())
                throw new IllegalStateException("Detenga la simulacion antes de cambiar la configuracion");
            Configuracion nueva = carga.getConfiguracion();
            Computador nuevo = new Computador(1, nueva.getRam()[0]);
            nuevo.cambiarPolitica(nueva.getPoliticas()[0], nueva.getQuantums()[0]);
            Cola<DefinicionProceso> definiciones = new Cola<>();
            for (DefinicionProceso definicion : carga.getProcesos()) {
                nuevo.recibirProceso(definicion.crearProceso());
                definiciones.agregar(definicion);
            }
            nuevo.admitirProcesos();
            ArchivoSistemaArchivos.SistemaCargado almacenamiento = crearAlmacenamiento(jsonSistemaArchivos);
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
        } finally { mutex.release(); }
    }

    private static ArchivoSistemaArchivos.SistemaCargado crearAlmacenamiento(String json) {
        if (json != null) return ArchivoSistemaArchivos.leer(json, SWAP_POR_DEFECTO);
        Disco vacio = new Disco(BLOQUES_POR_DEFECTO, SWAP_POR_DEFECTO, 0);
        return new ArchivoSistemaArchivos.SistemaCargado(vacio, new SistemaArchivos(vacio));
    }

    /**
     * Guarda el JSON del sistema de archivos para la proxima carga. Se valida al momento: si
     * esta mal formado no reemplaza al anterior. {@code null} vuelve al disco vacio.
     */
    public void cargarSistemaArchivos(String json) {
        if (json != null) crearAlmacenamiento(json);
        mutex.acquireUninterruptibly();
        try {
            if (cerrado) throw new IllegalStateException("El controlador esta cerrado");
            if (reloj != null && reloj.estaActivo())
                throw new IllegalStateException("Detenga la simulacion antes de cambiar el sistema de archivos");
            jsonSistemaArchivos = json;
        } finally { mutex.release(); }
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
                    quantum, new PoliticaPlanificacion[] {tipo}, new int[] {quantum});
        } finally { mutex.release(); }
    }

    public boolean cancelarProceso(Proceso proceso) {
        mutex.acquireUninterruptibly();
        try { comprobarDisponible(); return computador.terminarProceso(proceso); }
        finally { mutex.release(); }
    }

    public CargaInicial getCargaInicial() {
        mutex.acquireUninterruptibly();
        try {
            if (configuracion == null) throw new IllegalStateException("Configure primero la simulacion");
            Object[] elementos = cargaProcesos.instantanea();
            DefinicionProceso[] definiciones = new DefinicionProceso[elementos.length];
            for (int i = 0; i < elementos.length; i++) definiciones[i] = (DefinicionProceso) elementos[i];
            return new CargaInicial(configuracion, definiciones);
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
                    configuracion.getPoliticas(), configuracion.getQuantums());
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
