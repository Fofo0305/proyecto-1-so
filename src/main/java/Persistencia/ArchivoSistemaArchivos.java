package Persistencia;

import Modelo.PermisoArchivo;
import Sistema.Disco;
import Sistema.SistemaArchivos;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Carga el disco y el arbol de archivos desde el JSON del enunciado:
 * <pre>
 * {"diskConfig": {"totalBlocks": 200, "initialHead": 50},
 *  "fileSystem": [{"path": "/sistema", "type": "DIRECTORY", "owner": "admin", "permissions": "public"},
 *                 {"path": "/sistema/boot.bin", "type": "FILE", "startBlock": 11, "blocks": 1,
 *                  "owner": "admin", "permissions": "public"}]}
 * </pre>
 * {@code diskConfig.swapBlocks} y {@code process} (proceso creador de un archivo) son opcionales.
 * Cada archivo se restaura desde su {@code startBlock}, encadenado con los bloques libres siguientes.
 * @author Luciano
 */
public final class ArchivoSistemaArchivos {
    /** Proceso creador de los archivos que no indican uno: los carga el sistema. */
    public static final long PROCESO_SISTEMA = 0;

    private ArchivoSistemaArchivos() { }

    /** Disco y sistema de archivos cargados juntos, en modo administrador. */
    public record SistemaCargado(Disco disco, SistemaArchivos sistema) {
        public SistemaCargado {
            if (disco == null || sistema == null) throw new IllegalArgumentException("Carga incompleta");
        }
    }

    /** @param bloquesSwap tamano del area de intercambio si el JSON no lo indica */
    public static SistemaCargado cargar(Path origen, int bloquesSwap) throws IOException {
        if (origen == null) throw new IllegalArgumentException("El archivo es obligatorio");
        return leer(Files.readString(origen, StandardCharsets.UTF_8), bloquesSwap);
    }

    /** @param bloquesSwap tamano del area de intercambio si el JSON no lo indica */
    public static SistemaCargado leer(String json, int bloquesSwap) {
        if (json == null) throw new IllegalArgumentException("El JSON es obligatorio");
        try {
            JsonObject raiz = objeto(JsonParser.parseString(json), "el documento");
            JsonObject configuracion = objeto(raiz.get("diskConfig"), "diskConfig");
            int swap = configuracion.has("swapBlocks") ? entero(configuracion, "swapBlocks") : bloquesSwap;
            Disco disco = new Disco(entero(configuracion, "totalBlocks"), swap,
                    entero(configuracion, "initialHead"));
            SistemaArchivos sistema = new SistemaArchivos(disco);
            JsonObject[] entradas = entradas(raiz.get("fileSystem"));
            for (JsonObject entrada : entradas)
                if (esDirectorio(entrada) && !ruta(entrada).equals(SistemaArchivos.SEPARADOR))
                    sistema.crearDirectorio(ruta(entrada), texto(entrada, "owner"), permiso(entrada));
            for (JsonObject entrada : entradas)
                if (!esDirectorio(entrada))
                    sistema.restaurarArchivo(ruta(entrada), texto(entrada, "owner"), entero(entrada, "blocks"),
                            permiso(entrada), entrada.has("process") ? entero(entrada, "process") : PROCESO_SISTEMA,
                            entero(entrada, "startBlock"));
            return new SistemaCargado(disco, sistema);
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException error) {
            throw new IllegalArgumentException("JSON de sistema de archivos invalido: " + error.getMessage(), error);
        }
    }

    /**
     * Ordena las entradas para cargarlas: directorios de menor a mayor profundidad, para que
     * cada padre exista antes que sus hijos, y archivos por primer bloque, para que un archivo
     * que se monte sobre otro falle siempre igual.
     */
    private static JsonObject[] entradas(JsonElement elemento) {
        if (elemento == null || !elemento.isJsonArray())
            throw new IllegalArgumentException("fileSystem debe ser una lista");
        JsonArray lista = elemento.getAsJsonArray();
        JsonObject[] entradas = new JsonObject[lista.size()];
        for (int i = 0; i < entradas.length; i++) {
            entradas[i] = objeto(lista.get(i), "la entrada " + (i + 1));
            String tipo = texto(entradas[i], "type");
            if (!tipo.equals("DIRECTORY") && !tipo.equals("FILE"))
                throw new IllegalArgumentException("Tipo desconocido en " + ruta(entradas[i]) + ": " + tipo);
        }
        for (int i = 1; i < entradas.length; i++) {
            JsonObject actual = entradas[i];
            int j = i - 1;
            while (j >= 0 && orden(entradas[j]) > orden(actual)) {
                entradas[j + 1] = entradas[j];
                j--;
            }
            entradas[j + 1] = actual;
        }
        return entradas;
    }

    private static long orden(JsonObject entrada) {
        return esDirectorio(entrada) ? SistemaArchivos.normalizar(ruta(entrada)).split("/").length
                : Integer.MAX_VALUE + (long) entero(entrada, "startBlock");
    }

    private static boolean esDirectorio(JsonObject entrada) { return texto(entrada, "type").equals("DIRECTORY"); }

    private static String ruta(JsonObject entrada) { return texto(entrada, "path"); }

    private static PermisoArchivo permiso(JsonObject entrada) {
        String permiso = texto(entrada, "permissions");
        if (permiso.equals("public")) return PermisoArchivo.PUBLICO;
        if (permiso.equals("private")) return PermisoArchivo.PRIVADO;
        throw new IllegalArgumentException("Permiso desconocido en " + ruta(entrada) + ": " + permiso);
    }

    private static JsonObject objeto(JsonElement elemento, String nombre) {
        if (elemento == null || !elemento.isJsonObject())
            throw new IllegalArgumentException(nombre + " debe ser un objeto JSON");
        return elemento.getAsJsonObject();
    }

    private static String texto(JsonObject objeto, String campo) {
        JsonElement valor = objeto.get(campo);
        if (valor == null || !valor.isJsonPrimitive() || !((JsonPrimitive) valor).isString())
            throw new IllegalArgumentException("Falta el texto " + campo);
        return valor.getAsString();
    }

    private static int entero(JsonObject objeto, String campo) {
        JsonElement valor = objeto.get(campo);
        if (valor == null || !valor.isJsonPrimitive() || !((JsonPrimitive) valor).isNumber())
            throw new IllegalArgumentException("Falta el numero " + campo);
        double numero = valor.getAsDouble();
        if (numero != Math.rint(numero) || numero < Integer.MIN_VALUE || numero > Integer.MAX_VALUE)
            throw new IllegalArgumentException(campo + " debe ser entero");
        return (int) numero;
    }
}
