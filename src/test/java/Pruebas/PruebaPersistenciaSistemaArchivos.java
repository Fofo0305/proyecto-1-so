package Pruebas;

import Modelo.ModoUsuario;
import Modelo.PermisoArchivo;
import Modelo.TipoEntrada;
import Modelo.VistaAsignacionArchivo;
import Persistencia.ArchivoSistemaArchivos;
import Persistencia.ArchivoSistemaArchivos.SistemaCargado;
import Sistema.Disco;
import Sistema.SistemaArchivos;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Contrato del JSON del sistema de archivos: carga el ejemplo del enunciado, guarda sin dejar
 * temporales y vuelve a cargar los mismos bloques, y rechaza documentos corruptos, bloques
 * superpuestos y archivos fuera del area de datos.
 * @author Luciano
 */
public final class PruebaPersistenciaSistemaArchivos {
    private static final String DISCO_100 = "{\"totalBlocks\": 100, \"initialHead\": 0}";

    private PruebaPersistenciaSistemaArchivos() { }

    public static void main(String[] args) throws IOException {
        probarJsonDelEnunciado();
        probarGuardarYCargar();
        probarJsonCorrupto();
        probarBloquesSuperpuestos();
        probarAreaDatos();
        System.out.println("PruebaPersistenciaSistemaArchivos: OK");
    }

    private static void probarJsonDelEnunciado() throws IOException {
        SistemaCargado cargado = ArchivoSistemaArchivos.cargar(Path.of("ejemplos", "sistema-archivos.json"), 100);
        Disco disco = cargado.disco();
        SistemaArchivos sa = cargado.sistema();
        verificar(disco.getTotalBloques() == 200 && disco.getPosicionCabezal() == 50,
                "No respeta totalBlocks e initialHead del enunciado");
        verificar(disco.getTamanoSwap() == 19,
                "El swap no se recorto para que video.mp4 (bloque 180) quede en el area de datos");
        verificar(sa.tipoDe("/sistema") == TipoEntrada.DIRECTORIO && sa.tipoDe("/usuarios") == TipoEntrada.DIRECTORIO,
                "Faltan los directorios del enunciado");
        verificar(sa.consultarAsignacion().length == 8, "No cargo los ocho archivos del enunciado");
        verificar(iguales(sa.leer("/sistema/boot.bin"), 11) && iguales(sa.leer("/usuarios/video.mp4"), 180),
                "Un archivo no quedo en su startBlock");
        sa.cambiarSesion(ModoUsuario.USUARIO, "user2");
        verificar(!sa.puedeLeer("/usuarios/notas.txt") && sa.puedeLeer("/sistema/boot.bin"),
                "No se respetan los permisos del JSON");

        String json = Files.readString(Path.of("ejemplos", "sistema-archivos.json"), StandardCharsets.UTF_8);
        verificar(ArchivoSistemaArchivos.leer(json, 10).disco().getTamanoSwap() == 10,
                "Recorto un swap que ya cabia");
    }

    private static void probarGuardarYCargar() throws IOException {
        Disco disco = new Disco(100, 20, 37);
        SistemaArchivos sa = new SistemaArchivos(disco);
        sa.crearDirectorio("/home", "admin", PermisoArchivo.PUBLICO);
        sa.crearDirectorio("/home/ana", "ana", PermisoArchivo.PRIVADO);
        sa.crearArchivo("/a.bin", "admin", 2, PermisoArchivo.PUBLICO, 0);
        sa.crearArchivo("/home/ana/b.txt", "ana", 1, PermisoArchivo.PRIVADO, 4);
        sa.eliminar("/a.bin");
        sa.crearArchivo("/home/ana/c.dat", "ana", 3, PermisoArchivo.PRIVADO, 9);
        verificar(iguales(sa.leer("/home/ana/c.dat"), 0, 1, 3), "La prueba necesita un archivo fragmentado");

        Path carpeta = Files.createTempDirectory("prueba-json-sistema");
        Path destino = carpeta.resolve("sistema.json");
        try {
            Files.writeString(destino, "version anterior", StandardCharsets.UTF_8);
            ArchivoSistemaArchivos.guardar(new SistemaCargado(disco, sa), destino);
            String json = Files.readString(destino, StandardCharsets.UTF_8);
            verificar(json.contains("\"chain\"") && json.contains("\"swapBlocks\": 20"),
                    "El JSON guardado no trae la cadena ni el swap");
            try (Stream<Path> archivos = Files.list(carpeta)) {
                verificar(archivos.count() == 1, "Guardar dejo archivos temporales en la carpeta");
            }

            SistemaCargado otra = ArchivoSistemaArchivos.cargar(destino, 50);
            Disco disco2 = otra.disco();
            SistemaArchivos sa2 = otra.sistema();
            verificar(disco2.getTotalBloques() == 100 && disco2.getTamanoSwap() == 20
                    && disco2.getPosicionCabezal() == 37, "No volvio la configuracion del disco");
            verificar(iguales(sa2.leer("/home/ana/c.dat"), 0, 1, 3) && iguales(sa2.leer("/home/ana/b.txt"), 2),
                    "El archivo fragmentado no volvio a sus mismos bloques");
            VistaAsignacionArchivo[] filas = sa2.consultarAsignacion();
            verificar(filas.length == 2 && filas[0].dueno().equals("ana") && filas[0].procesoCreador() == 4
                    && filas[1].procesoCreador() == 9, "No volvieron el dueno y el proceso creador");
            verificar(ArchivoSistemaArchivos.escribir(otra).equals(json),
                    "Guardar lo que se cargo no da el mismo JSON");
            sa2.cambiarSesion(ModoUsuario.USUARIO, "luis");
            verificar(!sa2.puedeLeer("/home/ana/b.txt"), "Un archivo privado volvio publico");
        } finally {
            Files.deleteIfExists(destino);
            Files.deleteIfExists(carpeta);
        }
    }

    private static void probarJsonCorrupto() throws IOException {
        String ejemplo = Files.readString(Path.of("ejemplos", "sistema-archivos.json"), StandardCharsets.UTF_8);
        esperarRechazo(ejemplo.substring(0, ejemplo.length() / 2), null, "Acepto un JSON cortado a la mitad");
        esperarRechazo("{", null, "Acepto un JSON sin cerrar");
        esperarRechazo("[]", null, "Acepto una lista como documento");
        esperarRechazo("{\"fileSystem\": []}", null, "Acepto un JSON sin diskConfig");
        esperarRechazo("{\"diskConfig\": " + DISCO_100 + ", \"fileSystem\": {}}", null,
                "Acepto fileSystem que no es lista");
        esperarRechazo("{\"diskConfig\": {\"totalBlocks\": \"100\", \"initialHead\": 0}, \"fileSystem\": []}",
                null, "Acepto totalBlocks como texto");
        esperarRechazo(documento(DISCO_100, "{\"path\": \"/x\", \"type\": \"LINK\", \"owner\": \"admin\","
                + " \"permissions\": \"public\"}"), "LINK", "Acepto un tipo desconocido");
        esperarRechazo(documento(DISCO_100, archivo("/x", 1, 1, "").replace("public", "shared")), "shared",
                "Acepto un permiso desconocido");
        esperarRechazo(documento(DISCO_100, archivo("/x", 1, 1, "").replace("\"blocks\": 1", "\"blocks\": 1.5")),
                "blocks", "Acepto un tamano con decimales");
        esperarRechazo(documento(DISCO_100, archivo("/x", 1, 0, "")), "/x", "Acepto un archivo sin bloques");
        esperarRechazo(documento(DISCO_100, archivo("/x", 1, 2, ", \"chain\": [1]")), "/x",
                "Acepto una cadena mas corta que blocks");
        esperarRechazo(documento(DISCO_100, archivo("/x", 1, 1, ", \"chain\": [\"1\"]")), "chain",
                "Acepto una cadena con texto");
        esperarRechazo(documento(DISCO_100, archivo("/sin/padre.txt", 1, 1, "")), "/sin/padre.txt",
                "Acepto un archivo cuyo directorio no existe");
    }

    private static void probarBloquesSuperpuestos() {
        esperarRechazo(documento(DISCO_100, archivo("/a.txt", 5, 1, ""), archivo("/b.txt", 5, 1, "")),
                "/b.txt", "Acepto dos archivos en el mismo startBlock");
        esperarRechazo(documento(DISCO_100, archivo("/a.txt", 3, 2, ", \"chain\": [3, 4]"),
                archivo("/b.txt", 7, 2, ", \"chain\": [7, 4]")), "/b.txt", "Acepto dos cadenas con un bloque comun");
        esperarRechazo(documento(DISCO_100, archivo("/a.txt", 5, 2, ", \"chain\": [5, 5]")), "/a.txt",
                "Acepto una cadena que repite un bloque");
        esperarRechazo(documento(DISCO_100, archivo("/a.txt", 2, 3, ", \"chain\": [2, 3, 4]"),
                archivo("/b.txt", 3, 1, "")), "/b.txt", "Acepto un archivo encima de una cadena guardada");
        esperarRechazo(documento(DISCO_100, archivo("/a.txt", 1, 1, ""), archivo("/a.txt", 2, 1, "")),
                "/a.txt", "Acepto dos archivos con la misma ruta");

        SistemaCargado bien = ArchivoSistemaArchivos.leer(documento(DISCO_100,
                archivo("/a.txt", 1, 2, ""), archivo("/b.txt", 2, 1, ", \"chain\": [2]")), 20);
        verificar(iguales(bien.sistema().leer("/a.txt"), 1, 3) && iguales(bien.sistema().leer("/b.txt"), 2),
                "Sin cadena, un archivo no salto los bloques de una cadena guardada");
    }

    private static void probarAreaDatos() {
        esperarRechazo(documento("{\"totalBlocks\": 100, \"initialHead\": 0, \"swapBlocks\": 50}",
                archivo("/c.txt", 60, 1, "")), "area de datos", "Acepto un archivo dentro del swap");
        esperarRechazo(documento(DISCO_100, archivo("/c.txt", 99, 1, "")), "swap",
                "Acepto un disco sin lugar para el swap");
        esperarRechazo(documento(DISCO_100, archivo("/c.txt", -1, 1, "")), "/c.txt",
                "Acepto un startBlock negativo");
        esperarRechazo(documento(DISCO_100, archivo("/c.txt", 2147483647, 1, "")), "swap",
                "Un startBlock enorme desbordo la cuenta de bloques");
        SistemaCargado justo = ArchivoSistemaArchivos.leer(documento(DISCO_100, archivo("/c.txt", 97, 2, "")), 20);
        verificar(justo.disco().getTamanoSwap() == 1 && iguales(justo.sistema().leer("/c.txt"), 97, 98),
                "No dejo el swap en el ultimo bloque libre");
    }

    private static String documento(String configuracion, String... entradas) {
        return "{\"diskConfig\": " + configuracion + ", \"fileSystem\": [" + String.join(", ", entradas) + "]}";
    }

    private static String archivo(String ruta, int inicio, int bloques, String extra) {
        return "{\"path\": \"" + ruta + "\", \"type\": \"FILE\", \"startBlock\": " + inicio
                + ", \"blocks\": " + bloques + ", \"owner\": \"admin\", \"permissions\": \"public\"" + extra + "}";
    }

    /** @param fragmento texto que debe aparecer en el mensaje, o null si basta con que lo rechace */
    private static void esperarRechazo(String json, String fragmento, String mensaje) {
        try {
            ArchivoSistemaArchivos.leer(json, 20);
        } catch (IllegalArgumentException esperado) {
            String motivo = esperado.getMessage();
            verificar(motivo != null && !motivo.isBlank() && (fragmento == null || motivo.contains(fragmento)),
                    mensaje + " (motivo: " + motivo + ")");
            return;
        }
        throw new AssertionError(mensaje);
    }

    private static boolean iguales(int[] obtenidos, int... esperados) {
        if (obtenidos.length != esperados.length) return false;
        for (int i = 0; i < esperados.length; i++) if (obtenidos[i] != esperados[i]) return false;
        return true;
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
