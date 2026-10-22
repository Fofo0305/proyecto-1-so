package Modelo;

import EDD.Cola;
import java.util.concurrent.Semaphore;

/**
 * Almacenamiento FIFO de capacidad limitada de un computador anfitrion.
 * El mutex protege operaciones reales entre hilos; no representa los semaforos
 * simulados que bloquearan a los procesos productores y consumidores.
 * @author Luciano Rojas
 * @param <T> tipo de elemento almacenado
 */
public final class Buffer<T> {
    private final DefinicionBuffer definicion;
    private final Cola<T> elementos = new Cola<>();
    private final Semaphore mutex = new Semaphore(1, true);
    private final SemaforoSimulado mutexSimulado;
    private final SemaforoSimulado vacios;
    private final SemaforoSimulado llenos;
    private final RegistroEventosBuffer eventos;
    private long producidos;
    private long consumidos;

    public Buffer(String nombre, int capacidad, int anfitrion) {
        this.definicion = new DefinicionBuffer(nombre, capacidad, anfitrion);
        eventos = new RegistroEventosBuffer(nombre, 256);
        mutexSimulado = new SemaforoSimulado(nombre + ":mutex", 1, 1);
        vacios = new SemaforoSimulado(nombre + ":vacios", capacidad, capacidad);
        llenos = new SemaforoSimulado(nombre + ":llenos", 0, capacidad);
    }

    /**
     * Intenta insertar un elemento sin esperar espacio disponible.
     * @return false si el buffer esta lleno, sin modificar su contenido
     */
    public boolean insertar(T elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("El elemento no puede ser null");
        }
        mutex.acquireUninterruptibly();
        try {
            if (elementos.getTamano() == getCapacidad()) return false;
            elementos.agregar(elemento);
            producidos++;
            return true;
        } finally {
            mutex.release();
        }
    }

    /** Retira el elemento mas antiguo, o devuelve null si esta vacio. */
    public T extraer() {
        mutex.acquireUninterruptibly();
        try {
            T elemento = elementos.quitar();
            if (elemento != null) consumidos++;
            return elemento;
        } finally {
            mutex.release();
        }
    }

    /** Copia el contenido en orden FIFO sin exponer la cola interna. */
    public Object[] instantanea() {
        mutex.acquireUninterruptibly();
        try {
            return elementos.instantanea();
        } finally {
            mutex.release();
        }
    }

    public int getCantidad() {
        mutex.acquireUninterruptibly();
        try {
            return elementos.getTamano();
        } finally {
            mutex.release();
        }
    }

    public boolean estaVacio() { return getCantidad() == 0; }

    /** Cuenta modificaciones reales, incluso si su proceso termina antes de publicar el permiso. */
    public VistaContadoresBuffer consultarContadores() {
        mutex.acquireUninterruptibly();
        try {
            return new VistaContadoresBuffer(getNombre(), getAnfitrion(), getCapacidad(),
                    elementos.getTamano(), producidos, consumidos);
        } finally { mutex.release(); }
    }
    public boolean estaLleno() { return getCantidad() == getCapacidad(); }
    public String getNombre() { return definicion.nombre(); }
    public int getCapacidad() { return definicion.capacidad(); }
    public int getAnfitrion() { return definicion.anfitrion(); }
    public SemaforoSimulado getMutexSimulado() { return mutexSimulado; }
    public SemaforoSimulado getVacios() { return vacios; }
    public SemaforoSimulado getLlenos() { return llenos; }
    public void registrarEvento(long ciclo, int computador, long proceso, TipoEventoBuffer tipo, String detalle) {
        eventos.registrar(ciclo, computador, proceso, tipo, detalle);
    }
    public VistaEventosBuffer consultarEventos() { return eventos.consultar(); }
}
