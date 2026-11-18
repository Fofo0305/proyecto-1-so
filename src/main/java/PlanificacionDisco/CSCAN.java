package PlanificacionDisco;

import Modelo.DireccionDisco;
import Modelo.MovimientoCabezalDisco;
import Modelo.PoliticaDisco;
import Modelo.ResultadoPlanificacionDisco;

/** Barre en un sentido y salta al extremo opuesto sin sumar ese salto. @author Adolfo */
public final class CSCAN extends PoliticaBaseDisco {
    @Override public PoliticaDisco getTipo() { return PoliticaDisco.C_SCAN; }

    @Override public ResultadoPlanificacionDisco planificar(int posicionCabezal, int totalBloques,
            DireccionDisco direccion, int[] solicitudes) {
        int[] pendientes = validar(posicionCabezal, totalBloques, direccion, solicitudes);
        boolean sube = direccion == DireccionDisco.ASCENDENTE;
        int[] primerLado = SoporteBarridoDisco.lado(pendientes, posicionCabezal, sube, true, sube);
        int[] segundoLado = SoporteBarridoDisco.lado(pendientes, posicionCabezal, !sube, false, sube);
        int[] orden = SoporteBarridoDisco.combinar(primerLado, segundoLado);
        int extremo = sube ? totalBloques - 1 : 0;
        int salto = sube ? 0 : totalBloques - 1;
        int posicionTrasPrimero = primerLado.length == 0
                ? posicionCabezal : primerLado[primerLado.length - 1];
        boolean requiereExtremo = segundoLado.length > 0 && posicionTrasPrimero != extremo;
        int adicionales = segundoLado.length == 0 ? 0 : (requiereExtremo ? 2 : 1);
        MovimientoCabezalDisco[] movimientos = new MovimientoCabezalDisco[orden.length + adicionales];
        int indice = 0;
        for (int solicitud : primerLado)
            movimientos[indice++] = new MovimientoCabezalDisco(solicitud, true, true);
        if (segundoLado.length > 0) {
            if (requiereExtremo)
                movimientos[indice++] = new MovimientoCabezalDisco(extremo, true, false);
            movimientos[indice++] = new MovimientoCabezalDisco(salto, false, false);
        }
        for (int solicitud : segundoLado)
            movimientos[indice++] = new MovimientoCabezalDisco(solicitud, true, true);
        return resultadoRecorrido(posicionCabezal, direccion, orden, movimientos);
    }
}
