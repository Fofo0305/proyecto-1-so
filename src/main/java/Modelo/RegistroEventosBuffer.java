package Modelo;

import java.util.concurrent.Semaphore;

/** Historial circular acotado; leerlo nunca genera eventos nuevos. @author Luciano Rojas */
public final class RegistroEventosBuffer {
    private final String buffer;
    private final EventoBuffer[] eventos;
    private final Semaphore mutex = new Semaphore(1, true);
    private int inicio;
    private int cantidad;
    private long siguiente = 1;
    private long descartados;

    public RegistroEventosBuffer(String buffer, int capacidad) {
        if (buffer == null || buffer.isBlank() || capacidad < 1)
            throw new IllegalArgumentException("Nombre y capacidad del historial obligatorios");
        this.buffer = buffer;
        eventos = new EventoBuffer[capacidad];
    }

    public void registrar(long ciclo, int computador, long proceso, TipoEventoBuffer tipo, String detalle) {
        if (ciclo < 0 || computador < 1 || proceso < 1 || tipo == null || detalle == null)
            throw new IllegalArgumentException("Datos de evento invalidos");
        mutex.acquireUninterruptibly();
        try {
            EventoBuffer evento = new EventoBuffer(siguiente++, ciclo, buffer, computador, proceso, tipo, detalle);
            if (cantidad == eventos.length) {
                eventos[inicio] = evento;
                inicio = (inicio + 1) % eventos.length;
                descartados++;
            } else {
                eventos[(inicio + cantidad) % eventos.length] = evento;
                cantidad++;
            }
        } finally { mutex.release(); }
    }

    public VistaEventosBuffer consultar() {
        mutex.acquireUninterruptibly();
        try {
            EventoBuffer[] copia = new EventoBuffer[cantidad];
            for (int i = 0; i < cantidad; i++) copia[i] = eventos[(inicio + i) % eventos.length];
            return new VistaEventosBuffer(buffer, descartados, copia);
        } finally { mutex.release(); }
    }
}
