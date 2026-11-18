package PlanificacionDisco;

import Modelo.DireccionDisco;
import Modelo.MovimientoCabezalDisco;
import Modelo.ResultadoPlanificacionDisco;

/** Validacion y recorrido comun de las politicas del disco. @author Adolfo */
abstract class PoliticaBaseDisco implements PoliticaPlanificacionDisco {
    protected final int[] validar(int posicionCabezal, int totalBloques,
            DireccionDisco direccion, int[] solicitudes) {
        if (totalBloques < 1 || posicionCabezal < 0 || posicionCabezal >= totalBloques
                || direccion == null || solicitudes == null)
            throw new IllegalArgumentException("Parametros de planificacion invalidos");
        int[] copia = solicitudes.clone();
        for (int solicitud : copia)
            if (solicitud < 0 || solicitud >= totalBloques)
                throw new IllegalArgumentException("Solicitud fuera del disco: " + solicitud);
        return copia;
    }

    protected final ResultadoPlanificacionDisco resultadoDirecto(int posicionCabezal,
            DireccionDisco direccionInicial, int[] orden) {
        MovimientoCabezalDisco[] movimientos = new MovimientoCabezalDisco[orden.length];
        long desplazamiento = 0;
        int posicion = posicionCabezal;
        DireccionDisco direccion = direccionInicial;
        for (int i = 0; i < orden.length; i++) {
            int destino = orden[i];
            desplazamiento += Math.abs((long) destino - posicion);
            if (destino > posicion) direccion = DireccionDisco.ASCENDENTE;
            else if (destino < posicion) direccion = DireccionDisco.DESCENDENTE;
            movimientos[i] = new MovimientoCabezalDisco(destino, true, true);
            posicion = destino;
        }
        return new ResultadoPlanificacionDisco(orden, movimientos, desplazamiento, direccion);
    }
}
