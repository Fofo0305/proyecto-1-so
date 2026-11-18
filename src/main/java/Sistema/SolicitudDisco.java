package Sistema;

import Modelo.OperacionES;
import Modelo.VistaSolicitudDisco;

/** Estado interno de una solicitud mientras espera o se atiende. @author Adolfo */
final class SolicitudDisco {
    private final long id;
    private final long procesoId;
    private final OperacionES operacion;
    private final int bloqueObjetivo;
    private final int bloquesTotales;
    private final long cicloLlegada;
    private int bloquesRestantes;

    SolicitudDisco(long id, long procesoId, OperacionES operacion,
            int bloqueObjetivo, int bloquesTrabajo, long cicloLlegada) {
        this.id = id;
        this.procesoId = procesoId;
        this.operacion = operacion;
        this.bloqueObjetivo = bloqueObjetivo;
        bloquesTotales = bloquesTrabajo;
        bloquesRestantes = bloquesTrabajo;
        this.cicloLlegada = cicloLlegada;
    }

    void trabajarBloque() {
        if (bloquesRestantes < 1) throw new IllegalStateException("La solicitud ya termino");
        bloquesRestantes--;
    }

    boolean termino() { return bloquesRestantes == 0; }
    int getBloqueObjetivo() { return bloqueObjetivo; }

    VistaSolicitudDisco consultar() {
        return new VistaSolicitudDisco(id, procesoId, operacion, bloqueObjetivo,
                bloquesTotales, bloquesRestantes, cicloLlegada);
    }
}
