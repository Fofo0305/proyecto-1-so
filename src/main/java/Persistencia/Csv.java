package Persistencia;

import EDD.Cola;

/**
 * Codificación CSV con comas, comillas escapadas y campos multilínea.
 * @author Adolfo
 */
public final class Csv {
    private Csv() { }

    public static String registro(String... campos) {
        StringBuilder salida = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (campos[i] == null) throw new IllegalArgumentException("Campo CSV nulo");
            if (i > 0) salida.append(',');
            salida.append('"').append(campos[i].replace("\"", "\"\"")).append('"');
        }
        return salida.append('\n').toString();
    }

    public static RegistroCsv[] leer(String texto) {
        if (texto == null) throw new IllegalArgumentException("Texto CSV obligatorio");
        Cola<RegistroCsv> registros = new Cola<>();
        Cola<String> campos = new Cola<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        boolean comillasCerradas = false;
        int linea = 1;
        int inicio = 1;
        for (int i = 0; i < texto.length(); i++) {
            char caracter = texto.charAt(i);
            if (entreComillas) {
                if (caracter == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') { campo.append('"'); i++; }
                    else { entreComillas = false; comillasCerradas = true; }
                } else {
                    campo.append(caracter);
                    if (caracter == '\n' || (caracter == '\r' && (i + 1 == texto.length() || texto.charAt(i + 1) != '\n'))) linea++;
                }
            } else if (caracter == ',') {
                campos.agregar(campo.toString()); campo.setLength(0); comillasCerradas = false;
            } else if (caracter == '\n' || caracter == '\r') {
                campos.agregar(campo.toString()); campo.setLength(0);
                registros.agregar(new RegistroCsv(inicio, extraerCampos(campos)));
                comillasCerradas = false;
                if (caracter == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') i++;
                linea++; inicio = linea;
            } else if (caracter == '"' && campo.isEmpty() && !comillasCerradas) {
                entreComillas = true;
            } else {
                if (comillasCerradas || caracter == '"') throw error(linea, "Comillas o texto fuera de campo");
                campo.append(caracter);
            }
        }
        if (entreComillas) throw error(inicio, "Campo entre comillas sin cerrar");
        if (!campo.isEmpty() || comillasCerradas || !campos.estaVacia()) {
            campos.agregar(campo.toString());
            registros.agregar(new RegistroCsv(inicio, extraerCampos(campos)));
        }
        Object[] elementos = registros.instantanea();
        RegistroCsv[] resultado = new RegistroCsv[elementos.length];
        for (int i = 0; i < elementos.length; i++) resultado[i] = (RegistroCsv) elementos[i];
        return resultado;
    }

    private static String[] extraerCampos(Cola<String> campos) {
        String[] resultado = new String[campos.getTamano()];
        for (int i = 0; i < resultado.length; i++) resultado[i] = campos.quitar();
        return resultado;
    }

    private static IllegalArgumentException error(int linea, String motivo) {
        return new IllegalArgumentException("Linea " + linea + ": " + motivo);
    }
}
