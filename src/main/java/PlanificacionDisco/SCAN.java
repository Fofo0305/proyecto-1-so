package PlanificacionDisco;

import Modelo.DireccionDisco;
import Modelo.MovimientoCabezalDisco;
import Modelo.PoliticaDisco;
import Modelo.ResultadoPlanificacionDisco;

/** Barre hasta el extremo antes de invertir la direccion. @author Adolfo */
public final class SCAN extends PoliticaBaseDisco {
    @Override public PoliticaDisco getTipo() { return PoliticaDisco.SCAN; }

    @Override public ResultadoPlanificacionDisco planificar(int posicionCabezal, int totalBloques,
            DireccionDisco direccion, int[] solicitudes) {
        int[] pendientes = validar(posicionCabezal, totalBloques, direccion, solicitudes);
        boolean sube = direccion == DireccionDisco.ASCENDENTE;
        int[] primerLado = SoporteBarridoDisco.lado(pendientes, posicionCabezal, sube, true, sube);
        int[] segundoLado = SoporteBarridoDisco.lado(pendientes, posicionCabezal, !sube, false, !sube);
        int[] orden = SoporteBarridoDisco.combinar(primerLado, segundoLado);
        int extremo = sube ? totalBloques - 1 : 0;
        int posicionTrasPrimero = primerLado.length == 0
                ? posicionCabezal : primerLado[primerLado.length - 1];
        boolean requiereExtremo = segundoLado.length > 0 && posicionTrasPrimero != extremo;
        MovimientoCabezalDisco[] movimientos = new MovimientoCabezalDisco[
                orden.length + (requiereExtremo ? 1 : 0)];
        int indice = 0;
        for (int solicitud : primerLado)
            movimientos[indice++] = new MovimientoCabezalDisco(solicitud, true, true);
        if (requiereExtremo)
            movimientos[indice++] = new MovimientoCabezalDisco(extremo, true, false);
        for (int solicitud : segundoLado)
            movimientos[indice++] = new MovimientoCabezalDisco(solicitud, true, true);
        DireccionDisco finalDireccion = segundoLado.length == 0 ? direccion
                : sube ? DireccionDisco.DESCENDENTE : DireccionDisco.ASCENDENTE;
        return resultadoRecorrido(posicionCabezal, finalDireccion, orden, movimientos);
    }
}
