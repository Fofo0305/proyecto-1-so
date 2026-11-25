package Persistencia;

import Modelo.PermisoArchivo;
import Modelo.VistaAsignacionArchivo;
import Modelo.VistaEntradaArchivo;
import Sistema.Disco;
import Sistema.SistemaArchivos;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Carga el disco y el arbol de archivos desde el JSON del enunciado:
 * <pre>
 * {"diskConfig": {"totalBlocks": 200, "initialHead": 50},
 *  "fileSystem": [{"path": "/sistema", "type": "DIRECTORY", "owner": "admin", "permissions": "public"},
 *                 {"path": "/sistema/boot.bin", "type": "FILE", "startBlock": 11, "blocks": 1,
 *                  "owner": "admin", "permissions": "public"}]}
 * </pre>
 * {@code diskConfig.swapBlocks}, {@code process} (proceso creador de un archivo) y {@code chain}
 * (bloques exactos del archivo, en orden de enlace) son opcionales. Un archivo con {@code chain}
 * vuelve a esos bloques; uno sin ella se restaura desde su {@code startBlock}, encadenado con los
 * bloques libres siguientes. Al guardar se escriben los tres campos, para que el disco vuelva igual.
 * <p>El area swap ocupa los ultimos bloques del disco. Si el JSON no indica {@code swapBlocks},
 * se usa el tamano pedido, recortado para que todos los archivos queden en el area de datos.</p>
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

    /**
     * @param bloquesSwap tamano del area de intercambio si el JSON no lo indica; se recorta si
     *        los archivos llegan mas lejos
     */
    public static SistemaCargado leer(String json, int bloquesSwap) {
        if (json == null) throw new IllegalArgumentException("El JSON es obligatorio");
        try {
            JsonObject raiz = objeto(JsonParser.parseString(json), "el documento");
            JsonObject configuracion = objeto(raiz.get("diskConfig"), "diskConfig");
            JsonObject[] entradas = entradas(raiz.get("fileSystem"));
            int total = entero(configuracion, "totalBlocks");
            int swap = configuracion.has("swapBlocks") ? entero(configuracion, "swapBlocks")
                    : swapQueCabe(entradas, total, bloquesSwap);
            Disco disco = new Disco(total, swap, entero(configuracion, "initialHead"));
            validarAreaDatos(entradas, total - swap);
            SistemaArchivos sistema = new SistemaArchivos(disco);
            for (JsonObject entrada : entradas)
                if (esDirectorio(entrada) && !ruta(entrada).equals(SistemaArchivos.SEPARADOR))
                    sistema.crearDirectorio(ruta(entrada), texto(entrada, "owner"), permiso(entrada));
            for (JsonObject entrada : entradas) {
                if (esDirectorio(entrada)) continue;
                try {
                    restaurar(sistema, entrada);
                } catch (IllegalStateException | IllegalArgumentException error) {
                    throw new IllegalArgumentException("No se pudo restaurar " + ruta(entrada) + ": "
                            + error.getMessage(), error);
                }
            }
            return new SistemaCargado(disco, sistema);
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException error) {
            throw new IllegalArgumentException("JSON de sistema de archivos invalido: " + error.getMessage(), error);
        }
    }

    /**
     * Escribe el JSON en {@code destino} sin dejarlo a medias: lo escribe completo en un
     * temporal de la misma carpeta y luego lo renombra encima del destino. Si algo falla,
     * el archivo anterior queda intacto.
     */
    public static void guardar(SistemaCargado cargado, Path destino) throws IOException {
        if (destino == null) throw new IllegalArgumentException("El destino es obligatorio");
        String json = escribir(cargado);
        Path absoluto = destino.toAbsolutePath();
        Path carpeta = absoluto.getParent();
        if (carpeta == null) throw new IllegalArgumentException("El destino no tiene carpeta");
        Path temporal = Files.createTempFile(carpeta, absoluto.getFileName().toString() + ".", ".tmp");
        try {
            Files.writeString(temporal, json, StandardCharsets.UTF_8, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            try {
                Files.move(temporal, absoluto, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException sinRenombreAtomico) {
                Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    /** JSON del disco y del arbol: directorios en orden del arbol y luego archivos con su cadena. */
    public static String escribir(SistemaCargado cargado) {
        if (cargado == null) throw new IllegalArgumentException("El sistema es obligatorio");
        Disco disco = cargado.disco();
        JsonObject configuracion = new JsonObject();
        configuracion.addProperty("totalBlocks", disco.getTotalBloques());
        configuracion.addProperty("initialHead", disco.getPosicionCabezal());
        configuracion.addProperty("swapBlocks", disco.getTamanoSwap());
        JsonArray lista = new JsonArray();
        agregarDirectorios(cargado.sistema().consultarArbol(), lista);
        VistaAsignacionArchivo[] filas = cargado.sistema().consultarAsignacion();
        int archivos = agregarArchivos(cargado.sistema().consultarArbol(), filas, disco, lista, 0);
        if (archivos != filas.length)
            throw new IllegalStateException("El arbol cambio mientras se guardaba");
        JsonObject raiz = new JsonObject();
        raiz.add("diskConfig", configuracion);
        raiz.add("fileSystem", lista);
        return new GsonBuilder().setPrettyPrinting().create().toJson(raiz) + System.lineSeparator();
    }

    private static void agregarDirectorios(VistaEntradaArchivo nodo, JsonArray lista) {
        if (!nodo.esDirectorio()) return;
        if (!nodo.ruta().equals(SistemaArchivos.SEPARADOR)) lista.add(base(nodo, "DIRECTORY"));
        for (VistaEntradaArchivo hijo : nodo.hijos()) agregarDirectorios(hijo, lista);
    }

    /** Recorre el arbol en el mismo orden que la tabla de asignacion. @return archivos agregados */
    private static int agregarArchivos(VistaEntradaArchivo nodo, VistaAsignacionArchivo[] filas,
            Disco disco, JsonArray lista, int indice) {
        if (nodo.esDirectorio()) {
            for (VistaEntradaArchivo hijo : nodo.hijos())
                indice = agregarArchivos(hijo, filas, disco, lista, indice);
            return indice;
        }
        if (indice >= filas.length || !filas[indice].ruta().equals(nodo.ruta()))
            throw new IllegalStateException("El arbol cambio mientras se guardaba");
        JsonObject archivo = base(nodo, "FILE");
        archivo.addProperty("startBlock", nodo.primerBloque());
        archivo.addProperty("blocks", nodo.tamanoBloques());
        archivo.addProperty("process", filas[indice].procesoCreador());
        JsonArray cadena = new JsonArray();
        for (int bloque : disco.recorrerCadena(nodo.primerBloque())) cadena.add(bloque);
        archivo.add("chain", cadena);
        lista.add(archivo);
        return indice + 1;
    }

    private static JsonObject base(VistaEntradaArchivo nodo, String tipo) {
        JsonObject entrada = new JsonObject();
        entrada.addProperty("path", nodo.ruta());
        entrada.addProperty("type", tipo);
        entrada.addProperty("owner", nodo.dueno());
        entrada.addProperty("permissions", nodo.permiso() == PermisoArchivo.PUBLICO ? "public" : "private");
        return entrada;
    }

    /**
     * Swap pedido, recortado para que empiece despues del ultimo bloque que usan los archivos.
     * Un archivo sin cadena necesita al menos sus {@code blocks} bloques desde {@code startBlock}.
     */
    private static int swapQueCabe(JsonObject[] entradas, int total, int bloquesSwap) {
        long fin = 0;
        for (JsonObject entrada : entradas)
            if (!esDirectorio(entrada)) fin = Math.max(fin, finDeArchivo(entrada));
        long swap = Math.min(bloquesSwap, total - fin);
        if (swap < 1)
            throw new IllegalArgumentException("Los archivos llegan al bloque " + (fin - 1)
                    + " y no dejan espacio para el area swap");
        return (int) swap;
    }

    /** Rechaza, con la ruta del archivo, todo bloque que caiga fuera del area de datos. */
    private static void validarAreaDatos(JsonObject[] entradas, int inicioSwap) {
        for (JsonObject entrada : entradas) {
            if (esDirectorio(entrada)) continue;
            int inicio = entero(entrada, "startBlock");
            if (inicio < 0)
                throw new IllegalArgumentException("startBlock de " + ruta(entrada) + " no puede ser negativo");
            if (entero(entrada, "blocks") < 1)
                throw new IllegalArgumentException(ruta(entrada) + " necesita al menos un bloque");
            long fin = finDeArchivo(entrada);
            if (fin > inicioSwap)
                throw new IllegalArgumentException(ruta(entrada) + " usa el bloque " + (fin - 1)
                        + ", pero el area de datos termina en el bloque " + (inicioSwap - 1));
        }
    }

    /** Uno mas que el bloque mas alto que puede usar el archivo. */
    private static long finDeArchivo(JsonObject entrada) {
        if (!entrada.has("chain"))
            return (long) entero(entrada, "startBlock") + Math.max(1, entero(entrada, "blocks"));
        long fin = 0;
        for (int bloque : cadena(entrada)) {
            if (bloque < 0)
                throw new IllegalArgumentException("chain de " + ruta(entrada) + " tiene un bloque negativo");
            fin = Math.max(fin, bloque + 1);
        }
        return fin;
    }

    private static void restaurar(SistemaArchivos sistema, JsonObject entrada) {
        long proceso = entrada.has("process") ? entero(entrada, "process") : PROCESO_SISTEMA;
        int bloques = entero(entrada, "blocks");
        int inicio = entero(entrada, "startBlock");
        if (!entrada.has("chain")) {
            sistema.restaurarArchivo(ruta(entrada), texto(entrada, "owner"), bloques, permiso(entrada),
                    proceso, inicio);
            return;
        }
        int[] cadena = cadena(entrada);
        if (cadena.length != bloques || cadena[0] != inicio)
            throw new IllegalArgumentException("La cadena de " + ruta(entrada)
                    + " no coincide con startBlock y blocks");
        sistema.restaurarArchivo(ruta(entrada), texto(entrada, "owner"), permiso(entrada), proceso, cadena);
    }

    private static int[] cadena(JsonObject entrada) {
        JsonElement elemento = entrada.get("chain");
        if (!elemento.isJsonArray() || elemento.getAsJsonArray().isEmpty())
            throw new IllegalArgumentException("chain de " + ruta(entrada) + " debe ser una lista de bloques");
        JsonArray lista = elemento.getAsJsonArray();
        int[] cadena = new int[lista.size()];
        for (int i = 0; i < cadena.length; i++) cadena[i] = entero(lista.get(i), "chain");
        return cadena;
    }

    /**
     * Ordena las entradas para cargarlas: directorios de menor a mayor profundidad, para que
     * cada padre exista antes que sus hijos; luego los archivos con cadena, que ya saben sus
     * bloques, en el orden del documento para que el arbol vuelva igual; y al final los que
     * solo traen primer bloque, para que estos no tomen bloques de una cadena guardada. Estos
     * ultimos van por primer bloque, para que un archivo que se monte sobre otro falle siempre
     * igual. El ordenamiento es estable.
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
        if (esDirectorio(entrada)) return SistemaArchivos.normalizar(ruta(entrada)).split("/").length;
        if (entrada.has("chain")) return Integer.MAX_VALUE;
        return 2L * Integer.MAX_VALUE + entero(entrada, "startBlock");
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
        return entero(objeto.get(campo), campo);
    }

    private static int entero(JsonElement valor, String campo) {
        if (valor == null || !valor.isJsonPrimitive() || !((JsonPrimitive) valor).isNumber())
            throw new IllegalArgumentException("Falta el numero " + campo);
        double numero = valor.getAsDouble();
        if (numero != Math.rint(numero) || numero < Integer.MIN_VALUE || numero > Integer.MAX_VALUE)
            throw new IllegalArgumentException(campo + " debe ser entero");
        return (int) numero;
    }
}
