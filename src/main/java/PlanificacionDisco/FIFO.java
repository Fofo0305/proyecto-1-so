package PlanificacionDisco;

import Modelo.DireccionDisco;
import Modelo.PoliticaDisco;
import Modelo.ResultadoPlanificacionDisco;

/** Atiende las solicitudes en su orden de llegada. @author Adolfo */
public final class FIFO extends PoliticaBaseDisco {
    @Override public PoliticaDisco getTipo() { return PoliticaDisco.FIFO; }

    @Override public ResultadoPlanificacionDisco planificar(int posicionCabezal, int totalBloques,
            DireccionDisco direccion, int[] solicitudes) {
        int[] orden = validar(posicionCabezal, totalBloques, direccion, solicitudes);
        return resultadoDirecto(posicionCabezal, direccion, orden);
    }
}
