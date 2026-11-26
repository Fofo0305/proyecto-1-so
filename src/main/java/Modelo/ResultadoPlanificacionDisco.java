package Modelo;

/** Orden y recorrido inmutables decididos por una politica de disco. @author Adolfo */
public record ResultadoPlanificacionDisco(int[] orden, MovimientoCabezalDisco[] movimientos,
        long desplazamientoTotal, DireccionDisco direccionFinal) {
    public ResultadoPlanificacionDisco {
        if (orden == null || movimientos == null || desplazamientoTotal < 0 || direccionFinal == null)
            throw new IllegalArgumentException("Resultado de planificacion invalido");
        orden = orden.clone();
        movimientos = movimientos.clone();
        for (MovimientoCabezalDisco movimiento : movimientos)
            if (movimiento == null) throw new IllegalArgumentException("El recorrido contiene un movimiento nulo");
    }

    @Override public int[] orden() { return orden.clone(); }
    @Override public MovimientoCabezalDisco[] movimientos() { return movimientos.clone(); }
}
