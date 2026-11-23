package Pruebas;

import Modelo.PermisoArchivo;
import Modelo.TipoEntrada;
import Sistema.SistemaArchivos;

/** Contrato del arbol de directorios y de la resolucion de rutas. @author Luciano */
public final class PruebaSistemaArchivos {
    private PruebaSistemaArchivos() { }

    public static void main(String[] args) {
        probarArbolYRutas();
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
