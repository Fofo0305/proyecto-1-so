package Modelo;

/**
 * Configuración y carga iniciales inmutables para guardar y reconstruir simulaciones.
 * @author Adolfo
 */
public final class CargaInicial {
    private final Configuracion configuracion;
    private final DefinicionProceso[] procesos;
    private final DefinicionBuffer[] buffers;

    public CargaInicial(Configuracion configuracion, DefinicionProceso[] procesos, DefinicionBuffer[] buffers) {
        if (configuracion == null || procesos == null || buffers == null)
            throw new IllegalArgumentException("Configuracion, procesos y buffers son obligatorios");
        int[] ram = configuracion.getRam();
        for (DefinicionProceso proceso : procesos) {
            if (proceso == null || proceso.computador() > ram.length || proceso.memoria() > ram[proceso.computador() - 1])
                throw new IllegalArgumentException("Proceso sin anfitrion valido o con memoria superior a su RAM");
        }
        long[] reservas = new long[ram.length];
        for (int i = 0; i < buffers.length; i++) {
            DefinicionBuffer buffer = buffers[i];
            if (buffer == null || buffer.anfitrion() > ram.length) throw new IllegalArgumentException("Buffer sin anfitrion valido");
            for (int j = 0; j < i; j++)
                if (buffers[j].nombre().equals(buffer.nombre())) throw new IllegalArgumentException("Nombre de buffer duplicado");
            reservas[buffer.anfitrion() - 1] += buffer.capacidad();
            if (reservas[buffer.anfitrion() - 1] > ram[buffer.anfitrion() - 1])
                throw new IllegalArgumentException("Los buffers superan la RAM de su anfitrion");
        }
        this.configuracion = configuracion;
        for (DefinicionProceso proceso : procesos) {
            if (proceso.parametrosBuffer() == null) continue;
            boolean encontrado = false;
            for (DefinicionBuffer buffer : buffers)
                if (buffer.nombre().equals(proceso.parametrosBuffer().buffer())) encontrado = true;
            if (!encontrado) throw new IllegalArgumentException("El proceso requiere un buffer existente");
        }
        this.procesos = procesos.clone();
        this.buffers = buffers.clone();
    }

    public Configuracion getConfiguracion() { return configuracion; }
    public DefinicionProceso[] getProcesos() { return procesos.clone(); }
    public DefinicionBuffer[] getBuffers() { return buffers.clone(); }
}
