package Modelo;

/** Metricas y serie temporal inmutables del disco. @author Adolfo */
public record VistaMetricasDisco(long ciclo, long desplazamientoTotal,
        long solicitudesAtendidas, double esperaPromedio, long suspensiones,
        long reactivaciones, long[] atendidasPorPolitica, long[] ciclos,
        long[] desplazamientoAcumulado) {
    public VistaMetricasDisco {
        if (atendidasPorPolitica == null || ciclos == null || desplazamientoAcumulado == null
                || ciclos.length != desplazamientoAcumulado.length)
            throw new IllegalArgumentException("Metricas de disco invalidas");
        atendidasPorPolitica = atendidasPorPolitica.clone();
        ciclos = ciclos.clone();
        desplazamientoAcumulado = desplazamientoAcumulado.clone();
    }

    @Override public long[] atendidasPorPolitica() { return atendidasPorPolitica.clone(); }
    @Override public long[] ciclos() { return ciclos.clone(); }
    @Override public long[] desplazamientoAcumulado() { return desplazamientoAcumulado.clone(); }

    public long atendidas(PoliticaDisco politica) {
        if (politica == null) throw new IllegalArgumentException("La politica es obligatoria");
        return atendidasPorPolitica[politica.ordinal()];
    }
}
