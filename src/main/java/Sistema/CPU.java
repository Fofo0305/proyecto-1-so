package Sistema;

import Modelo.EstadoProceso;
import Modelo.ModoCPU;
import Modelo.MotivoBloqueo;
import Modelo.Proceso;
import Modelo.TipoProceso;

/**
 * Ejecuta como máximo una instrucción por ciclo. El núcleo serializa su acceso.
 * @author Adolfo
 */
public class CPU implements ComponenteReloj {
    private Proceso actual;
    private ModoCPU modo = ModoCPU.SISTEMA_OPERATIVO;
    private long ultimoCiclo;

    /** Asigna un proceso listo; FCFS lo conserva hasta terminar. */
    public void asignar(Proceso proceso) {
        if (actual != null) throw new IllegalStateException("La CPU esta ocupada");
        if (proceso == null || proceso.getTipo() == TipoProceso.IO_BOUND
                || proceso.getEstado() != EstadoProceso.LISTO
                || proceso.getRestante() == 0 || proceso.getDeadline() == 0)
            throw new IllegalArgumentException("Se requiere un proceso ejecutable listo y vigente");
        proceso.cambiarEstado(EstadoProceso.EJECUCION, MotivoBloqueo.NINGUNO);
        actual = proceso;
    }

    /** Ejecuta una instrucción de usuario, o mantiene modo sistema si está desocupada. */
    public void avanzarCiclo(long ciclo) {
        if (ciclo != ultimoCiclo + 1) throw new IllegalArgumentException("El ciclo debe ser consecutivo");
        if (actual == null) {
            modo = ModoCPU.SISTEMA_OPERATIVO;
        } else {
            actual.ejecutarInstruccion();
            modo = ModoCPU.USUARIO;
        }
        ultimoCiclo = ciclo;
    }

    /** Ejecuta una unica instruccion de sistema bajo el bloqueo del computador. */
    public void avanzarCicloSistema(long ciclo, Runnable instruccion) {
        if (ciclo != ultimoCiclo + 1 || actual == null || instruccion == null)
            throw new IllegalArgumentException("Se requiere un ciclo consecutivo y una instruccion en CPU");
        modo = ModoCPU.SISTEMA_OPERATIVO;
        instruccion.run();
        ultimoCiclo = ciclo;
    }

    /** Retira el contexto; el núcleo gestiona su estado y la memoria. */
    public Proceso retirar() {
        Proceso retirado = actual;
        actual = null;
        return retirado;
    }

    public Proceso getActual() { return actual; }
    public ModoCPU getModo() { return modo; }
    public long getUltimoCiclo() { return ultimoCiclo; }
}
