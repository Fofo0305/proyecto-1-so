package Modelo;

/** Copia inmutable de un bloque para pruebas e interfaz. @author Adolfo */
public record VistaBloqueDisco(int numero, AreaDisco area, String propietario, int siguiente) {
    public VistaBloqueDisco {
        if (numero < 0 || area == null || siguiente < -1 || propietario != null && propietario.isBlank()
                || propietario == null && siguiente != -1)
            throw new IllegalArgumentException("Vista de bloque invalida");
    }

    public boolean libre() { return propietario == null; }
}
