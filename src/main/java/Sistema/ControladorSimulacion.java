package Sistema;

import Modelo.Configuracion;
import Modelo.Proceso;
import Modelo.TipoProceso;
import Modelo.VistaComputador;
import Modelo.CargaInicial;
import Modelo.DefinicionProceso;
import Modelo.DefinicionBuffer;
import Modelo.Buffer;
import Modelo.SemaforoSimulado;
import Modelo.ParametrosBuffer;
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
    private Cola<Buffer<Object>> buffers = new Cola<>();

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
            Cola<Buffer<Object>> nuevosBuffers = new Cola<>();
            for (DefinicionBuffer buffer : carga.getBuffers())
                registrarBuffer(buffer, nuevos, nuevosBuffers);
            Cola<DefinicionProceso> definiciones = new Cola<>();
            for (DefinicionProceso definicion : carga.getProcesos()) {
                Buffer<Object> buffer = definicion.parametrosBuffer() == null ? null
                        : buscarBuffer(definicion.parametrosBuffer().buffer(), nuevosBuffers);
                nuevos[definicion.computador() - 1].recibirProceso(definicion.crearProceso(buffer));
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
            buffers = nuevosBuffers;
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

    /** Crea un productor o consumidor local sin modificar recursos si la solicitud es invalida. */
    public Proceso crearProcesoBuffer(String nombre, int computador, int prioridad, int memoria,
            int deadline, TipoProceso tipo, String nombreBuffer, int intervalo, int elementos) {
        ParametrosBuffer parametros = new ParametrosBuffer(nombreBuffer, intervalo, elementos);
        DefinicionProceso definicion = new DefinicionProceso(nombre, computador, prioridad,
                memoria, parametros.instrucciones(), deadline, tipo, parametros);
        mutex.acquireUninterruptibly();
        try {
            comprobarDisponible();
            if (computador < 1 || computador > computadores.length)
                throw new IllegalArgumentException("Seleccione un computador existente");
            Computador destino = computadores[computador - 1];
            if (memoria > destino.getMemoria().getCapacidad())
                throw new IllegalArgumentException("La memoria del proceso supera la RAM del computador");
            Buffer<Object> buffer = buscarBuffer(nombreBuffer, buffers);
            Proceso proceso = definicion.crearProceso(buffer);
            destino.recibirProceso(proceso);
            destino.admitirProcesos();
            cargaProcesos.agregar(definicion);
            return proceso;
        } finally { mutex.release(); }
    }

    private static Buffer<Object> buscarBuffer(String nombre, Cola<Buffer<Object>> registro) {
        for (Object elemento : registro.instantanea()) {
            @SuppressWarnings("unchecked") Buffer<Object> buffer = (Buffer<Object>) elemento;
            if (buffer.getNombre().equals(nombre)) return buffer;
        }
        throw new IllegalArgumentException("El buffer indicado no existe");
    }

    /** Devuelve atributos iniciales; nunca guarda PC, estado o deadline ya consumido. */
    public CargaInicial getCargaInicial() {
        mutex.acquireUninterruptibly();
        try {
            if (configuracion == null) throw new IllegalStateException("Configure primero el cluster");
            Object[] elementos = cargaProcesos.instantanea();
            DefinicionProceso[] definiciones = new DefinicionProceso[elementos.length];
            for (int i = 0; i < elementos.length; i++) definiciones[i] = (DefinicionProceso) elementos[i];
            Object[] registrados = buffers.instantanea();
            DefinicionBuffer[] cargaBuffers = new DefinicionBuffer[registrados.length];
            for (int i = 0; i < registrados.length; i++) {
                Buffer<?> buffer = (Buffer<?>) registrados[i];
                cargaBuffers[i] = new DefinicionBuffer(buffer.getNombre(), buffer.getCapacidad(), buffer.getAnfitrion());
            }
            return new CargaInicial(configuracion, definiciones, cargaBuffers);
        } finally { mutex.release(); }
    }

    /** Registra un buffer y reserva una unidad de RAM por posicion en su anfitrion. */
    public Buffer<Object> crearBuffer(String nombre, int capacidad, int anfitrion) {
        mutex.acquireUninterruptibly();
        try {
            comprobarDisponible();
            return registrarBuffer(new DefinicionBuffer(nombre, capacidad, anfitrion), computadores, buffers);
        } finally { mutex.release(); }
    }

    private static Buffer<Object> registrarBuffer(DefinicionBuffer definicion,
            Computador[] equipos, Cola<Buffer<Object>> registro) {
        if (definicion.anfitrion() > equipos.length)
            throw new IllegalArgumentException("Seleccione un computador anfitrion existente");
        for (Object elemento : registro.instantanea())
            if (((Buffer<?>) elemento).getNombre().equals(definicion.nombre()))
                throw new IllegalArgumentException("Nombre de buffer duplicado");
        Buffer<Object> buffer = new Buffer<>(definicion.nombre(), definicion.capacidad(), definicion.anfitrion());
        Modelo.Memoria memoria = equipos[definicion.anfitrion() - 1].getMemoria();
        String propietario = "buffer:" + definicion.nombre();
        if (!memoria.reservar(propietario, definicion.capacidad()))
            throw new IllegalArgumentException("No hay RAM para el buffer " + definicion.nombre());
        try {
            registro.agregar(buffer);
        } catch (RuntimeException | Error fallo) {
            memoria.liberar(propietario);
            throw fallo;
        }
        return buffer;
    }

    /** Devuelve un arreglo independiente; los buffers pertenecen al controlador. */
    public Buffer<?>[] getBuffers() {
        mutex.acquireUninterruptibly();
        try {
            Object[] registrados = buffers.instantanea();
            Buffer<?>[] copia = new Buffer<?>[registrados.length];
            for (int i = 0; i < copia.length; i++) copia[i] = (Buffer<?>) registrados[i];
            return copia;
        } finally { mutex.release(); }
    }

    /** Solicita wait sobre uno de los semaforos de buffers de la sesion actual. */
    public boolean esperarSemaforo(Proceso proceso, SemaforoSimulado semaforo) {
        mutex.acquireUninterruptibly();
        try {
            comprobarDisponible();
            comprobarSemaforo(semaforo);
            if (proceso == null || proceso.getComputador() < 1 || proceso.getComputador() > computadores.length)
                throw new IllegalArgumentException("El proceso debe pertenecer al cluster");
            return computadores[proceso.getComputador() - 1].esperarSemaforo(proceso, semaforo);
        } finally { mutex.release(); }
    }

    /** Libera un permiso y devuelve el proceso despertado, o null si no hubo espera. */
    public Proceso senalizarSemaforo(SemaforoSimulado semaforo) {
        mutex.acquireUninterruptibly();
        try {
            comprobarDisponible();
            comprobarSemaforo(semaforo);
            Proceso receptor;
            while ((receptor = semaforo.senalizar()) != null) {
                int equipo = receptor.getComputador();
                if (equipo >= 1 && equipo <= computadores.length
                        && computadores[equipo - 1].despertarPorSemaforo(receptor, semaforo)) return receptor;
                // El plazo puede vencer entre retirar la espera y adquirir el mutex del nucleo.
                // Se devuelve el mismo permiso mediante otra senal, sin incrementar dos veces.
            }
            return null;
        } finally { mutex.release(); }
    }

    private void comprobarSemaforo(SemaforoSimulado semaforo) {
        for (Object elemento : buffers.instantanea()) {
            Buffer<?> buffer = (Buffer<?>) elemento;
            if (semaforo == buffer.getMutexSimulado() || semaforo == buffer.getVacios()
                    || semaforo == buffer.getLlenos()) return;
        }
        throw new IllegalArgumentException("El semaforo debe pertenecer a un buffer de la sesion actual");
    }

    public void iniciar() {
        mutex.acquireUninterruptibly();
        try { comprobarDisponible(); reloj.iniciar(); pausado = false; }
        finally { mutex.release(); }
    }

    public Modelo.VistaBuffer[] consultarBuffers() {
        mutex.acquireUninterruptibly();
        try {
            Object[] registrados = buffers.instantanea();
            Modelo.VistaBuffer[] vistas = new Modelo.VistaBuffer[registrados.length];
            for (int i = 0; i < vistas.length; i++) {
                Buffer<?> buffer = (Buffer<?>) registrados[i];
                vistas[i] = computadores[buffer.getAnfitrion() - 1].consultarBuffer(buffer);
            }
            return vistas;
        } finally { mutex.release(); }
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
