package Persistencia;

import Modelo.CargaInicial;
import Modelo.Configuracion;
import Modelo.DefinicionProceso;
import Modelo.ModoUsuario;
import Modelo.OperacionES;
import Modelo.ParametrosArchivo;
import Modelo.PermisoArchivo;
import Modelo.PoliticaDisco;
import Modelo.PoliticaPlanificacion;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static Persistencia.CamposJson.entero;
import static Persistencia.CamposJson.objeto;
import static Persistencia.CamposJson.presente;
import static Persistencia.CamposJson.texto;

/**
 * Lee el JSON de procesos del enunciado: {@code globalConfig} (ciclo, swap y politica de
 * disco inicial), {@code computer} (RAM, politica de planificacion y quantum) y la lista
 * {@code processes}. Cada proceso con {@code targetPath} y {@code operation} es un proceso
 * de archivo lanzado por el administrador; sin esos campos es un proceso de CPU.
 * @author Luciano
 */
public final class ArchivoProcesos {
    public static final int CICLO_POR_DEFECTO = 700;
    public static final int QUANTUM_POR_DEFECTO = 4;

    private ArchivoProcesos() { }

    public static CargaInicial cargar(Path archivo) throws IOException {
        if (archivo == null) throw new IllegalArgumentException("La ruta es obligatoria");
        return leer(Files.readString(archivo, StandardCharsets.UTF_8));
    }

    public static CargaInicial leer(String json) {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("JSON de procesos invalido: esta vacio");
        try {
            JsonObject raiz = objeto(JsonParser.parseString(json), "El JSON de procesos");
            Configuracion configuracion = configuracion(raiz);
            DefinicionProceso[] procesos = procesos(raiz, configuracion.getRam()[0]);
            return new CargaInicial(configuracion, procesos);
        } catch (JsonParseException | IllegalStateException error) {
            throw new IllegalArgumentException("JSON de procesos invalido: " + error.getMessage(), error);
        } catch (IllegalArgumentException error) {
            String mensaje = error.getMessage();
            if (mensaje != null && mensaje.startsWith("JSON de procesos invalido")) throw error;
            throw new IllegalArgumentException("JSON de procesos invalido: " + mensaje, error);
        }
    }

    private static Configuracion configuracion(JsonObject raiz) {
        JsonObject global = presente(raiz, "globalConfig") ? objeto(raiz.get("globalConfig"), "globalConfig") : new JsonObject();
        int ciclo = CamposJson.entero(global, "cycleDurationMs", CICLO_POR_DEFECTO);
        int swap = CamposJson.entero(global, "swapSizeBlocks", Configuracion.SWAP_POR_DEFECTO);
        PoliticaDisco politicaDisco = politicaDisco(CamposJson.texto(global, "initialDiskPolicy", "FIFO"));

        JsonObject computador = objeto(raiz.get("computer"), "computer");
        int ram = entero(computador, "ramCapacity");
        PoliticaPlanificacion politica = politica(CamposJson.texto(computador, "policy", "FCFS"));
        int quantum = CamposJson.entero(computador, "quantum", 0);
        if (quantum < 1) {
            if (politica == PoliticaPlanificacion.ROUND_ROBIN)
                throw new IllegalArgumentException("Round Robin necesita un quantum de al menos 1");
            quantum = QUANTUM_POR_DEFECTO;
        }
        return new Configuracion(new int[] {ram}, ciclo, quantum,
                new PoliticaPlanificacion[] {politica}, new int[] {quantum})
                .conDisco(Configuracion.BLOQUES_DISCO_POR_DEFECTO, swap, politicaDisco, 0);
    }

    private static DefinicionProceso[] procesos(JsonObject raiz, int ram) {
        JsonElement lista = raiz.get("processes");
        if (lista == null || !lista.isJsonArray()) throw new IllegalArgumentException("Falta la lista processes");
        JsonArray arreglo = lista.getAsJsonArray();
        DefinicionProceso[] procesos = new DefinicionProceso[arreglo.size()];
        for (int indice = 0; indice < procesos.length; indice++) {
            JsonObject entrada = objeto(arreglo.get(indice), "El proceso " + (indice + 1));
            String nombre = presente(entrada, "name") ? texto(entrada, "name") : "proceso " + (indice + 1);
            try {
                procesos[indice] = proceso(entrada, ram);
            } catch (IllegalArgumentException | IllegalStateException error) {
                throw new IllegalArgumentException(nombre + ": " + error.getMessage(), error);
            }
        }
        return procesos;
    }

    private static DefinicionProceso proceso(JsonObject entrada, int ram) {
        String nombre = texto(entrada, "name");
        int memoria = entero(entrada, "memory");
        if (memoria > ram)
            throw new IllegalArgumentException("pide " + memoria + " de memoria y la RAM es de " + ram);
        int instrucciones = entero(entrada, "instructions");
        int deadline = entero(entrada, "deadline");
        int prioridad = CamposJson.entero(entrada, "priority", 0);
        boolean conRuta = presente(entrada, "targetPath");
        boolean conOperacion = presente(entrada, "operation");
        if (!conRuta && !conOperacion)
            return new DefinicionProceso(nombre, 1, prioridad, memoria, instrucciones, deadline);
        if (conRuta != conOperacion)
            throw new IllegalArgumentException("targetPath y operation van juntos");
        OperacionES operacion = operacion(texto(entrada, "operation"));
        ParametrosArchivo parametros = ParametrosArchivo.ADMINISTRADOR;
        if (operacion == OperacionES.CREAR) {
            int bloques = CamposJson.entero(entrada, "fileSizeBlocks", 0);
            if (bloques < 1) throw new IllegalArgumentException("CREATE necesita fileSizeBlocks de al menos 1");
            parametros = new ParametrosArchivo(bloques, permiso(CamposJson.texto(entrada, "permissions", "public")),
                    null, ModoUsuario.ADMINISTRADOR, "admin");
        }
        return new DefinicionProceso(nombre, 1, prioridad, memoria, instrucciones, deadline,
                operacion, texto(entrada, "targetPath"), parametros);
    }

    private static String clave(String texto) {
        return texto.replace("-", "").replace("_", "").replace(" ", "").toUpperCase(java.util.Locale.ROOT);
    }

    private static PoliticaPlanificacion politica(String nombre) {
        switch (clave(nombre)) {
            case "FCFS": return PoliticaPlanificacion.FCFS;
            case "RR": case "ROUNDROBIN": return PoliticaPlanificacion.ROUND_ROBIN;
            case "EDF": return PoliticaPlanificacion.EDF;
            case "PRIORIDADES": case "PRIORIDAD": case "PRIORITY": return PoliticaPlanificacion.PRIORIDADES;
            default: throw new IllegalArgumentException("Politica de planificacion desconocida: " + nombre);
        }
    }

    private static PoliticaDisco politicaDisco(String nombre) {
        switch (clave(nombre)) {
            case "FIFO": return PoliticaDisco.FIFO;
            case "SSTF": return PoliticaDisco.SSTF;
            case "SCAN": return PoliticaDisco.SCAN;
            case "CSCAN": return PoliticaDisco.C_SCAN;
            default: throw new IllegalArgumentException("Politica de disco desconocida: " + nombre);
        }
    }

    private static OperacionES operacion(String nombre) {
        switch (clave(nombre)) {
            case "READ": case "LEER": return OperacionES.LEER;
            case "UPDATE": case "ACTUALIZAR": return OperacionES.ACTUALIZAR;
            case "DELETE": case "ELIMINAR": return OperacionES.ELIMINAR;
            case "CREATE": case "CREAR": return OperacionES.CREAR;
            default: throw new IllegalArgumentException("Operacion desconocida: " + nombre);
        }
    }

    private static PermisoArchivo permiso(String nombre) {
        switch (clave(nombre)) {
            case "PUBLIC": case "PUBLICO": return PermisoArchivo.PUBLICO;
            case "PRIVATE": case "PRIVADO": return PermisoArchivo.PRIVADO;
            default: throw new IllegalArgumentException("Permiso desconocido: " + nombre);
        }
    }
}
