package Modelo;

import EDD.Cola;
import java.util.concurrent.Semaphore;

/**
 * Semaforo de AvilaOS con permisos no negativos y espera FIFO de procesos.
 * Nunca espera por un permiso simulado: el nucleo realiza bloqueo y despertar.
 * El semaforo de Java solo protege los datos durante operaciones breves.
 * @author Luciano Rojas
 */
public final class SemaforoSimulado {
    private final String nombre;
    private final int maximo;
    private final Semaphore mutex = new Semaphore(1, true);
    private final Cola<Proceso> esperando = new Cola<>();
    private int valor;

    public SemaforoSimulado(String nombre, int inicial, int maximo) {
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("El nombre del semaforo es obligatorio");
        if (maximo < 1 || inicial < 0 || inicial > maximo)
            throw new IllegalArgumentException("Permisos iniciales fuera de rango");
        this.nombre = nombre;
        this.valor = inicial;
        this.maximo = maximo;
    }

    /**
     * Consume un permiso o encola el proceso una unica vez y devuelve false.
     * Un proceso despertado recibe el permiso directamente: no repite este wait.
     * El nucleo debe serializar esta llamada con la transicion a BLOQUEADO.
     */
    public boolean esperar(Proceso proceso) {
        if (proceso == null) throw new IllegalArgumentException("El proceso es obligatorio");
        mutex.acquireUninterruptibly();
        try {
            for (Object elemento : esperando.instantanea())
                if (elemento == proceso) throw new IllegalStateException("El proceso ya espera en este semaforo");
            if (valor > 0) {
                valor--;
                return true;
            }
            esperando.agregar(proceso);
            return false;
        } finally { mutex.release(); }
    }

    /**
     * Transfiere un permiso al primer proceso o incrementa el valor si no hay espera.
     * El llamador despierta al receptor fuera del mutex. Si ya termino, debe volver
     * a senalizar para entregar ese permiso a otro proceso o devolverlo al contador.
     */
    public Proceso senalizar() {
        mutex.acquireUninterruptibly();
        try {
            if (!esperando.estaVacia()) return esperando.quitar();
            if (valor == maximo) throw new IllegalStateException("El semaforo ya tiene todos sus permisos");
            valor++;
            return null;
        } finally { mutex.release(); }
    }

    /** Retira una espera cancelada sin crear permisos ni alterar el orden relativo. */
    public boolean cancelarEspera(Proceso proceso) {
        if (proceso == null) throw new IllegalArgumentException("El proceso es obligatorio");
        mutex.acquireUninterruptibly();
        try {
            boolean retirado = false;
            int cantidad = esperando.getTamano();
            for (int i = 0; i < cantidad; i++) {
                Proceso actual = esperando.quitar();
                if (actual == proceso) retirado = true;
                else esperando.agregar(actual);
            }
            return retirado;
        } finally { mutex.release(); }
    }

    /** Copia FIFO de las referencias; el nucleo conserva los contextos de procesos. */
    public Proceso[] getEsperando() {
        mutex.acquireUninterruptibly();
        try {
            Object[] elementos = esperando.instantanea();
            Proceso[] copia = new Proceso[elementos.length];
            for (int i = 0; i < copia.length; i++) copia[i] = (Proceso) elementos[i];
            return copia;
        } finally { mutex.release(); }
    }

    public int getValor() {
        mutex.acquireUninterruptibly();
        try { return valor; } finally { mutex.release(); }
    }

    public String getNombre() { return nombre; }
    public int getMaximo() { return maximo; }
}
