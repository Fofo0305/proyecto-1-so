package Sistema;

import EDD.Cola;
import Modelo.Proceso;
import Modelo.SemaforoSimulado;

/**
 * Entrega permisos remotos en la barrera global, sin adquirir el mutex de otro
 * computador desde un trabajador. El receptor puede ejecutar desde el ciclo siguiente.
 * @author Luciano Rojas
 */
public final class InterconexionBuffers implements FinalizadorCiclo {
    private record Transferencia(Proceso proceso, SemaforoSimulado semaforo) { }
    private final Cola<Transferencia> pendientes = new Cola<>();
    private final java.util.concurrent.Semaphore mutexPendientes = new java.util.concurrent.Semaphore(1, true);
    private Computador[] equipos;

    public void conectar(Computador[] equipos) {
        if (this.equipos != null || equipos == null || equipos.length < 2)
            throw new IllegalArgumentException("La interconexion requiere un cluster nuevo");
        this.equipos = equipos.clone();
    }

    public void enviar(Proceso proceso, SemaforoSimulado semaforo) {
        if (equipos == null || proceso == null || semaforo == null
                || proceso.getComputador() < 1 || proceso.getComputador() > equipos.length)
            throw new IllegalArgumentException("Transferencia sin destino valido");
        mutexPendientes.acquireUninterruptibly();
        try { pendientes.agregar(new Transferencia(proceso, semaforo)); }
        finally { mutexPendientes.release(); }
    }

    /**
     * Retira transferencias aun en cola y devuelve sus permisos al nucleo para que
     * los redistribuya. Una transferencia ya extraida pertenece al finalizador:
     * este comprobara el estado del receptor y devolvera el permiso si termino.
     */
    public SemaforoSimulado[] cancelarPendientes(Proceso proceso) {
        if (proceso == null) throw new IllegalArgumentException("El proceso es obligatorio");
        mutexPendientes.acquireUninterruptibly();
        try {
            int cantidad = pendientes.getTamano();
            SemaforoSimulado[] retirados = new SemaforoSimulado[cantidad];
            int total = 0;
            for (int i = 0; i < cantidad; i++) {
                Transferencia transferencia = pendientes.quitar();
                if (transferencia.proceso() == proceso) retirados[total++] = transferencia.semaforo();
                else pendientes.agregar(transferencia);
            }
            SemaforoSimulado[] permisos = new SemaforoSimulado[total];
            System.arraycopy(retirados, 0, permisos, 0, total);
            return permisos;
        } finally { mutexPendientes.release(); }
    }

    public int getCantidadPendientes() {
        mutexPendientes.acquireUninterruptibly();
        try { return pendientes.getTamano(); }
        finally { mutexPendientes.release(); }
    }

    private Transferencia siguiente() {
        mutexPendientes.acquireUninterruptibly();
        try { return pendientes.quitar(); }
        finally { mutexPendientes.release(); }
    }

    @Override public void finalizarCiclo(long ciclo) {
        Transferencia transferencia;
        while ((transferencia = siguiente()) != null) {
            Proceso receptor = transferencia.proceso();
            while (receptor != null) {
                if (equipos[receptor.getComputador() - 1].despertarPorSemaforo(receptor, transferencia.semaforo())) break;
                // El destinatario pudo vencer antes de recibir el permiso: no se pierde.
                receptor = transferencia.semaforo().senalizar();
            }
        }
    }
}
