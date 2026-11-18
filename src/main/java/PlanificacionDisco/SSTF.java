package PlanificacionDisco;

import Modelo.DireccionDisco;
import Modelo.PoliticaDisco;
import Modelo.ResultadoPlanificacionDisco;

/** Elige repetidamente la solicitud mas cercana al cabezal. @author Adolfo */
public final class SSTF extends PoliticaBaseDisco {
    @Override public PoliticaDisco getTipo() { return PoliticaDisco.SSTF; }

    @Override public ResultadoPlanificacionDisco planificar(int posicionCabezal, int totalBloques,
            DireccionDisco direccion, int[] solicitudes) {
        int[] pendientes = validar(posicionCabezal, totalBloques, direccion, solicitudes);
        int[] orden = new int[pendientes.length];
        boolean[] atendida = new boolean[pendientes.length];
        int posicion = posicionCabezal;
        for (int turno = 0; turno < orden.length; turno++) {
            int elegida = -1;
            long distanciaMenor = Long.MAX_VALUE;
            for (int i = 0; i < pendientes.length; i++) {
                if (atendida[i]) continue;
                long distancia = Math.abs((long) pendientes[i] - posicion);
                if (distancia < distanciaMenor) {
                    elegida = i;
                    distanciaMenor = distancia;
                }
            }
            orden[turno] = pendientes[elegida];
            atendida[elegida] = true;
            posicion = pendientes[elegida];
        }
        return resultadoDirecto(posicionCabezal, direccion, orden);
    }
}
