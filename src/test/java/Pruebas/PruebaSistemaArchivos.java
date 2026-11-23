package Pruebas;

import Modelo.AreaDisco;
import Modelo.PermisoArchivo;
import Modelo.VistaBloqueDisco;
import Modelo.TipoEntrada;
import Sistema.Disco;
import Sistema.SistemaArchivos;

/** Contrato del arbol de directorios, las rutas y la asignacion encadenada. @author Luciano */
public final class PruebaSistemaArchivos {
    private PruebaSistemaArchivos() { }

    public static void main(String[] args) {
        probarArbolYRutas();
        probarAsignacionEncadenada();
        System.out.println("PruebaSistemaArchivos: OK");
    }

    private static void probarArbolYRutas() {
        SistemaArchivos sa = new SistemaArchivos();
        verificar(sa.existe("/") && sa.tipoDe("/") == TipoEntrada.DIRECTORIO, "La raiz no existe");
        sa.crearDirectorio("/home", "admin", PermisoArchivo.PUBLICO);
        sa.crearDirectorio("/home/luciano", "luciano", PermisoArchivo.PRIVADO);
        sa.crearDirectorio("//home///adolfo/", "adolfo", PermisoArchivo.PUBLICO);
        verificar(sa.existe("/home/luciano") && sa.existe("/home/adolfo/"), "No resuelve rutas creadas");
        verificar(!sa.existe("/home/maria") && !sa.existe("/tmp/x"), "Resuelve rutas inexistentes");
        String[] hijos = sa.listar("/home");
        verificar(hijos.length == 2 && hijos[0].equals("luciano") && hijos[1].equals("adolfo"),
                "listar no respeta el orden de creacion");
        verificar(sa.getTotalEntradas() == 4, "El arbol no cuenta sus entradas");
        verificar(SistemaArchivos.normalizar("//home//luciano/").equals("/home/luciano"),
                "normalizar no limpia la ruta");
        verificar(SistemaArchivos.normalizar("/").equals("/"), "normalizar cambia la raiz");

        esperarInvalida(() -> sa.crearDirectorio("/home/luciano", "luciano", PermisoArchivo.PUBLICO),
                "Acepta directorios duplicados");
        esperarInvalida(() -> sa.crearDirectorio("/var/log", "admin", PermisoArchivo.PUBLICO),
                "Crea un directorio sin padre");
        esperarInvalida(() -> sa.crearDirectorio("home", "admin", PermisoArchivo.PUBLICO),
                "Acepta rutas relativas");
        esperarInvalida(() -> sa.crearDirectorio("/home/../etc", "admin", PermisoArchivo.PUBLICO),
                "Acepta .. en la ruta");
        esperarInvalida(() -> sa.crearDirectorio("/", "admin", PermisoArchivo.PUBLICO),
                "Permite recrear la raiz");
        esperarInvalida(() -> sa.listar("/no/existe"), "Lista un directorio inexistente");
    }

    private static void probarAsignacionEncadenada() {
        Disco disco = new Disco(100, 20, 0);
        verificar(disco.contarLibres(AreaDisco.DATOS) == 80, "El area de datos no tiene 80 bloques");
        int a = disco.asignarEncadenado("F1", 3);
        int b = disco.asignarEncadenado("F2", 2);
        verificar(a == 0 && b == 3, "La asignacion no usa primer ajuste");
        verificar(iguales(disco.recorrerCadena(a), 0, 1, 2), "La cadena de F1 no enlaza 0-1-2");
        VistaBloqueDisco ultimo = disco.consultarBloques()[2];
        verificar(ultimo.propietario().equals("F1") && ultimo.siguiente() == -1,
                "El ultimo bloque no cierra la cadena con -1");

        verificar(disco.liberarEncadenado(a) == 3, "No libera todos los bloques de la cadena");
        verificar(disco.consultarBloques()[0].libre() && disco.contarLibres(AreaDisco.DATOS) == 78,
                "Los bloques liberados no vuelven a estar libres");
        int c = disco.asignarEncadenado("F3", 5);
        verificar(iguales(disco.recorrerCadena(c), 0, 1, 2, 5, 6),
                "La cadena no salta los bloques ocupados por otro archivo");
        verificar(iguales(disco.recorrerCadena(b), 3, 4), "Asignar F3 altero la cadena de F2");

        try {
            disco.asignarEncadenado("F4", 74);
            throw new AssertionError("Asigna mas bloques de los libres");
        } catch (IllegalStateException esperado) {
            verificar(disco.contarLibres(AreaDisco.DATOS) == 73, "Un fallo de espacio ocupo bloques");
        }
        verificar(disco.contarLibres(AreaDisco.SWAP) == 20, "La asignacion de archivos invadio el swap");
        esperarInvalida(() -> disco.recorrerCadena(50), "Recorre una cadena desde un bloque libre");
        esperarInvalida(() -> disco.asignarEncadenado("F5", 0), "Acepta archivos de cero bloques");
    }

    private static boolean iguales(int[] obtenidos, int... esperados) {
        if (obtenidos.length != esperados.length) return false;
        for (int i = 0; i < esperados.length; i++) if (obtenidos[i] != esperados[i]) return false;
        return true;
    }

    static void esperarInvalida(Runnable accion, String mensaje) {
        try {
            accion.run();
            throw new AssertionError(mensaje);
        } catch (IllegalArgumentException esperado) {
            // Se rechazo como corresponde.
        }
    }

    static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
