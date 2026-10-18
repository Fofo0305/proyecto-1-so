package Sistema;

import Modelo.Configuracion;
import Modelo.Proceso;
import Modelo.TipoProceso;
import Modelo.VistaComputador;
import Modelo.CargaInicial;
import Modelo.DefinicionProceso;
import Modelo.DefinicionBuffer;
import EDD.Cola;
import java.util.concurrent.Semaphore;

/**
 * Construye y conserva los componentes de la simulación fuera de las ventanas.
 * Las operaciones que esperan hilos deben ejecutarse fuera del EDT de Swing.
 * @author Adolfo
 */
public class ControladorSimulacion implements AutoCloseable {
    private final Semaphore mutex = new Semaphore(1, true);
    private Configuracion configuracion;
    private Computador[] computadores;
    private RelojGlobal reloj;
    private boolean cerrado;
    private boolean pausado;
    private boolean detenido;
    private Cola<DefinicionProceso> cargaProcesos = new Cola<>();
    private DefinicionBuffer[] cargaBuffers = new DefinicionBuffer[0];

    /** Reemplaza la simulación únicamente si el reloj anterior está detenido o no iniciado. */
    public void configurar(Configuracion nueva) {
        cargarCarga(new CargaInicial(nueva, new DefinicionProceso[0], new DefinicionBuffer[0]));
    }

    /** Valida y construye la carga completa antes de reemplazar la sesión anterior. */
    public void cargarCarga(CargaInicial carga) {
        if (carga == null) throw new IllegalArgumentException("La carga es obligatoria");
        mutex.acquireUninterruptibly();
        try {
            if (cerrado) throw new IllegalStateException("El controlador esta cerrado");
            if (reloj != null && reloj.estaActivo())
                throw new IllegalStateException("Detenga la simulacion antes de cambiar la configuracion");
            Configuracion nueva = carga.getConfiguracion();
            int[] capacidades = nueva.getRam();
            Computador[] nuevos = new Computador[capacidades.length];
            for (int i = 0; i < nuevos.length; i++) nuevos[i] = new Computador(i + 1, capacidades[i]);
            for (DefinicionBuffer buffer : carga.getBuffers())
                if (!nuevos[buffer.anfitrion() - 1].getMemoria().reservar("buffer:" + buffer.nombre(), buffer.capacidad()))
                    throw new IllegalArgumentException("No hay RAM para el buffer " + buffer.nombre());
            Cola<DefinicionProceso> definiciones = new Cola<>();
            for (DefinicionProceso definicion : carga.getProcesos()) {
                nuevos[definicion.computador() - 1].recibirProceso(definicion.crearProceso());
                definiciones.agregar(definicion);
            }
            for (Computador computador : nuevos) computador.admitirProcesos();
            RelojGlobal nuevoReloj = new RelojGlobal(nuevos);
            nuevoReloj.setDuracionMs(nueva.getDuracionMs());
            if (reloj != null) reloj.close();
            configuracion = nueva;
            computadores = nuevos;
            reloj = nuevoReloj;
            pausado = false;
            detenido = false;
            cargaProcesos = definiciones;
            cargaBuffers = carga.getBuffers();
        } finally { mutex.release(); }
    }

    /** Crea un proceso CPU-bound en el computador elegido y solicita su admisión. */
    public Proceso crearProceso(String nombre, int computador, int prioridad,
                                int memoria, int instrucciones, int deadline) {
        mutex.acquireUninterruptibly();
        try {
            if (cerrado || computadores == null || detenido) throw new IllegalStateException("Configure primero el cluster");
            if (computador < 1 || computador > computadores.length)
                throw new IllegalArgumentException("Seleccione un computador existente");
            Computador destino = computadores[computador - 1];
            if (memoria > destino.getMemoria().getCapacidad())
                throw new IllegalArgumentException("La memoria del proceso supera la RAM total del computador");
            DefinicionProceso definicion = new DefinicionProceso(nombre, computador, prioridad, memoria, instrucciones, deadline);
            Proceso proceso = definicion.crearProceso();
            destino.recibirProceso(proceso);
            destino.admitirProcesos();
            cargaProcesos.agregar(definicion);
            return proceso;
        } finally { mutex.release(); }
    }

    /** Devuelve atributos iniciales; nunca guarda PC, estado o deadline ya consumido. */
    public CargaInicial getCargaInicial() {
        mutex.acquireUninterruptibly();
        try {
            if (configuracion == null) throw new IllegalStateException("Configure primero el cluster");
            Object[] elementos = cargaProcesos.instantanea();
            DefinicionProceso[] definiciones = new DefinicionProceso[elementos.length];
            for (int i = 0; i < elementos.length; i++) definiciones[i] = (DefinicionProceso) elementos[i];
            return new CargaInicial(configuracion, definiciones, cargaBuffers);
        } finally { mutex.release(); }
    }

    public void iniciar() {
        mutex.acquireUninterruptibly();
        try { comprobarDisponible(); reloj.iniciar(); pausado = false; }
        finally { mutex.release(); }
    }

    public void pausar() {
        mutex.acquireUninterruptibly();
        try { comprobarDisponible(); reloj.pausar(); pausado = true; }
        finally { mutex.release(); }
    }

    public void reanudar() {
        mutex.acquireUninterruptibly();
        try { comprobarDisponible(); reloj.reanudar(); pausado = false; }
        finally { mutex.release(); }
    }

    /** Detener es definitivo para el reloj actual; configurar crea una sesión nueva. */
    public void detener() {
        mutex.acquireUninterruptibly();
        try {
            if (cerrado || reloj == null) throw new IllegalStateException("Configure primero el cluster");
            reloj.detener();
            detenido = true;
            pausado = false;
        } finally { mutex.release(); }
    }

    public void cambiarDuracion(int duracionMs) {
        mutex.acquireUninterruptibly();
        try {
            comprobarDisponible();
            Configuracion actualizada = new Configuracion(configuracion.getRam(), duracionMs,
                    configuracion.getQuantum(), configuracion.getLatencia());
            reloj.setDuracionMs(duracionMs);
            configuracion = actualizada;
        } finally { mutex.release(); }
    }

    private void comprobarDisponible() {
        if (cerrado || reloj == null || detenido) throw new IllegalStateException("Configure un cluster disponible");
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

    public Configuracion getConfiguracion() {
        mutex.acquireUninterruptibly();
        try { return configuracion; } finally { mutex.release(); }
    }

    /** Devuelve un arreglo independiente; las instancias pertenecen al controlador. */
    public Computador[] getComputadores() {
        mutex.acquireUninterruptibly();
        try { return computadores == null ? new Computador[0] : computadores.clone(); }
        finally { mutex.release(); }
    }

    /** Captura las vistas sin exponer procesos mutables a la interfaz. */
    public VistaComputador[] consultarVistas() {
        mutex.acquireUninterruptibly();
        try {
            if (computadores == null) return new VistaComputador[0];
            VistaComputador[] vistas = new VistaComputador[computadores.length];
            for (int i = 0; i < vistas.length; i++) vistas[i] = computadores[i].consultarVista();
            return vistas;
        } finally { mutex.release(); }
    }

    public long getCiclo() {
        mutex.acquireUninterruptibly();
        try { return reloj == null ? 0 : reloj.getCiclo(); } finally { mutex.release(); }
    }

    @Override
    public void close() {
        mutex.acquireUninterruptibly();
        try {
            cerrado = true;
            if (reloj != null) reloj.close();
        } finally { mutex.release(); }
    }
}
