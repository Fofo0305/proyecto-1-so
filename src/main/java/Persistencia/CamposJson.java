package Persistencia;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Lectura de campos JSON con mensajes que dicen que campo falta o esta mal. La usan los
 * lectores del sistema de archivos y de los procesos.
 * @author Luciano
 */
final class CamposJson {
    private CamposJson() { }

    static JsonObject objeto(JsonElement elemento, String nombre) {
        if (elemento == null || !elemento.isJsonObject())
            throw new IllegalArgumentException(nombre + " debe ser un objeto JSON");
        return elemento.getAsJsonObject();
    }

    static String texto(JsonObject objeto, String campo) {
        JsonElement valor = objeto.get(campo);
        if (valor == null || !valor.isJsonPrimitive() || !((JsonPrimitive) valor).isString())
            throw new IllegalArgumentException("Falta el texto " + campo);
        return valor.getAsString();
    }

    static int entero(JsonObject objeto, String campo) {
        return entero(objeto.get(campo), campo);
    }

    static int entero(JsonElement valor, String campo) {
        if (valor == null || !valor.isJsonPrimitive() || !((JsonPrimitive) valor).isNumber())
            throw new IllegalArgumentException("Falta el numero " + campo);
        double numero = valor.getAsDouble();
        if (numero != Math.rint(numero) || numero < Integer.MIN_VALUE || numero > Integer.MAX_VALUE)
            throw new IllegalArgumentException(campo + " debe ser entero");
        return (int) numero;
    }

    /** El numero del campo, o {@code porDefecto} si el campo no esta o es null. */
    static int entero(JsonObject objeto, String campo, int porDefecto) {
        return presente(objeto, campo) ? entero(objeto, campo) : porDefecto;
    }

    /** El texto del campo, o {@code porDefecto} si el campo no esta o es null. */
    static String texto(JsonObject objeto, String campo, String porDefecto) {
        return presente(objeto, campo) ? texto(objeto, campo) : porDefecto;
    }

    static boolean presente(JsonObject objeto, String campo) {
        return objeto.has(campo) && !objeto.get(campo).isJsonNull();
    }
}
