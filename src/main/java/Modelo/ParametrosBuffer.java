package Modelo;

/** Carga inicial de un productor o consumidor. @author Luciano Rojas */
public record ParametrosBuffer(String buffer, int intervalo, int elementos) {
    public ParametrosBuffer {
        if (buffer == null || buffer.isBlank() || intervalo < 1 || elementos < 1)
            throw new IllegalArgumentException("Buffer, intervalo y elementos deben ser validos y positivos");
        if ((long) elementos * (intervalo + 5L) > Integer.MAX_VALUE)
            throw new IllegalArgumentException("La carga de productor consumidor supera el rango de instrucciones");
    }

    /** Cada elemento requiere intervalo ciclos de usuario y cinco instrucciones de sistema. */
    public int instrucciones() { return (int) ((long) elementos * (intervalo + 5L)); }
}
