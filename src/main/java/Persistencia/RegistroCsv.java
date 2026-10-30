package Persistencia;

/**
 * Campos de un registro CSV y número de línea donde empieza.
 * @author Adolfo
 */
public record RegistroCsv(int linea, String[] campos) {
    public RegistroCsv { campos = campos.clone(); }
    @Override
    public String[] campos() { return campos.clone(); }
}
