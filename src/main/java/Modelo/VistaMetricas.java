package Modelo;

/**
 * Lectura inmutable de las metricas de un computador.
 *
 * La ocupacion incluye las instrucciones de usuario y las operaciones de sistema de los
 * procesos. La equidad de Jain usa las instrucciones recibidas por todos los procesos
 * creados en el equipo.
 * @author Antony
 */
public record VistaMetricas(
        int computador, long ciclos, long ocupados,
        int creados, int completados, int vencidos, int cancelados,
        int respondidos, double respuesta, double equidad,
        double servicio, double servicioCuadrado,
        long[] historiaCiclos, double[] historiaUso) {

    public VistaMetricas {
        historiaCiclos = historiaCiclos.clone();
        historiaUso = historiaUso.clone();
    }

    @Override
    public long[] historiaCiclos() {
        return historiaCiclos.clone();
    }

    @Override
    public double[] historiaUso() {
        return historiaUso.clone();
    }

    /** Procesos completados por ciclo. */
    public double throughput() {
        return ciclos == 0 ? 0 : (double) completados / ciclos;
    }

    /** Porcentaje de ciclos con la CPU ocupada. */
    public double utilizacion() {
        return ciclos == 0 ? 0 : 100.0 * ocupados / ciclos;
    }

    /** Porcentaje de procesos terminados a tiempo sobre los que llegaron a su plazo. */
    public double cumplimiento() {
        return completados + vencidos == 0 ? 0 : 100.0 * completados / (completados + vencidos);
    }
}
