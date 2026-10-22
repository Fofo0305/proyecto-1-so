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
        pendientes.agregar(new Transferencia(proceso, semaforo));
    }

    @Override public void finalizarCiclo(long ciclo) {
        Transferencia transferencia;
        while ((transferencia = pendientes.quitar()) != null) {
            Proceso receptor = transferencia.proceso();
            while (receptor != null) {
                if (equipos[receptor.getComputador() - 1].despertarPorSemaforo(receptor, transferencia.semaforo())) break;
                // El destinatario pudo vencer antes de recibir el permiso: no se pierde.
                receptor = transferencia.semaforo().senalizar();
            }
        }
    }
}
