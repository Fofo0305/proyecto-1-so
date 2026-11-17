package Modelo;

/** Punto del recorrido producido por una politica de disco. @author Adolfo */
public record MovimientoCabezalDisco(int destino, boolean contabilizaDesplazamiento,
        boolean atiendeSolicitud) {
    public MovimientoCabezalDisco {
        if (destino < 0) throw new IllegalArgumentException("El destino no puede ser negativo");
    }
}
