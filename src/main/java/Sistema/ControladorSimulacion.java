package Sistema;

import EDD.Cola;
import Modelo.CargaInicial;
import Modelo.Configuracion;
import Modelo.DefinicionProceso;
import Modelo.EventoSistema;
import Modelo.ParametrosES;
import Modelo.PoliticaPlanificacion;
import Modelo.Proceso;
import Modelo.TipoProceso;
import Modelo.VistaComputador;
import Modelo.VistaMetricas;
import java.util.concurrent.Semaphore;

/** Controla la única instancia del computador sin depender de Swing. @author Adolfo */
public class ControladorSimulacion implements AutoCloseable {
    private final Semaphore mutex = new Semaphore(1, true);
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
            RelojGlobal nuevoReloj = new RelojGlobal(new ComponenteReloj[] {nuevo});
            nuevoReloj.setDuracionMs(nueva.getDuracionMs());
            if (reloj != null) reloj.close();
            configuracion = nueva;
            computador = nuevo;
            reloj = nuevoReloj;
            pausado = false;
            detenido = false;
            cargaProcesos = definiciones;
        } finally { mutex.release(); }
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
                    quantum, 0, new PoliticaPlanificacion[] {tipo}, new int[] {quantum});
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
            configuracion = new Configuracion(configuracion.getRam(), duracionMs, configuracion.getQuantum(), 0,
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
    public long getCiclo() { mutex.acquireUninterruptibly(); try { return reloj == null ? 0 : reloj.getCiclo(); } finally { mutex.release(); } }

    @Override public void close() {
        mutex.acquireUninterruptibly();
        try { cerrado = true; if (reloj != null) reloj.close(); }
        finally { mutex.release(); }
    }
}
