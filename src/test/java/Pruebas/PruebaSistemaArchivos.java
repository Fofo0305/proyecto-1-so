package Pruebas;

import Modelo.AreaDisco;
import Modelo.ModoUsuario;
import Modelo.PermisoArchivo;
import Modelo.VistaAsignacionArchivo;
import Modelo.VistaBloqueDisco;
import Modelo.VistaEntradaArchivo;
import Modelo.TipoEntrada;
import Sistema.Disco;
import Sistema.SistemaArchivos;

/**
 * Contrato del arbol de directorios, las rutas, la asignacion encadenada, el CRUD, el borrado
 * recursivo, los permisos por modo, la falta de espacio, la fragmentacion y las vistas.
 * @author Luciano
 */
public final class PruebaSistemaArchivos {
    private PruebaSistemaArchivos() { }

    public static void main(String[] args) {
        probarArbolYRutas();
        probarAsignacionEncadenada();
        probarCrud();
        probarEliminacionRecursiva();
        probarPermisos();
        probarFaltaDeEspacioYFragmentacion();
        probarVistas();
        System.out.println("PruebaSistemaArchivos: OK");
    }

    private static void probarArbolYRutas() {
        SistemaArchivos sa = new SistemaArchivos(new Disco(100, 20, 0));
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

    private static void probarCrud() {
        Disco disco = new Disco(100, 20, 0);
        SistemaArchivos sa = new SistemaArchivos(disco);
        sa.crearDirectorio("/home", "admin", PermisoArchivo.PUBLICO);
        int primero = sa.crearArchivo("/home/notas.txt", "luciano", 4, PermisoArchivo.PRIVADO, 7);
        sa.crearArchivo("/home/foto.png", "luciano", 2, PermisoArchivo.PUBLICO, 8);
        verificar(primero == 0 && sa.tipoDe("/home/notas.txt") == TipoEntrada.ARCHIVO,
                "crearArchivo no registra el archivo");
        verificar(iguales(sa.leer("/home/notas.txt"), 0, 1, 2, 3), "leer no recorre la cadena del archivo");
        verificar(disco.consultarBloques()[0].propietario().startsWith("F"),
                "El disco no marca el bloque con el archivo");

        sa.renombrar("/home/notas.txt", "apuntes.txt");
        verificar(!sa.existe("/home/notas.txt") && sa.existe("/home/apuntes.txt"), "renombrar no cambia el nombre");
        verificar(iguales(sa.leer("/home/apuntes.txt"), 0, 1, 2, 3), "renombrar movio los bloques");
        sa.renombrar("/home", "usuarios");
        verificar(sa.existe("/usuarios/foto.png"), "Renombrar un directorio pierde a sus hijos");
        esperarInvalida(() -> sa.renombrar("/usuarios/foto.png", "apuntes.txt"), "Renombra sobre un nombre usado");
        esperarInvalida(() -> sa.renombrar("/usuarios/foto.png", "a/b"), "Acepta nombres con separador");
        esperarInvalida(() -> sa.renombrar("/", "raiz"), "Renombra la raiz");

        int libresAntes = disco.contarLibres(AreaDisco.DATOS);
        try {
            sa.crearArchivo("/usuarios/enorme.bin", "luciano", libresAntes + 1, PermisoArchivo.PUBLICO, 9);
            throw new AssertionError("Crea un archivo sin bloques suficientes");
        } catch (IllegalStateException esperado) {
            verificar(!sa.existe("/usuarios/enorme.bin") && disco.contarLibres(AreaDisco.DATOS) == libresAntes,
                    "Un archivo sin espacio dejo rastros");
        }
        esperarInvalida(() -> sa.crearArchivo("/usuarios/foto.png", "luciano", 1, PermisoArchivo.PUBLICO, 9),
                "Crea un archivo duplicado");
        esperarInvalida(() -> sa.crearArchivo("/usuarios/foto.png/x", "luciano", 1, PermisoArchivo.PUBLICO, 9),
                "Crea un archivo dentro de otro archivo");
        esperarInvalida(() -> sa.leer("/usuarios"), "Lee un directorio como archivo");

        sa.eliminar("/usuarios/apuntes.txt");
        verificar(!sa.existe("/usuarios/apuntes.txt") && disco.contarLibres(AreaDisco.DATOS) == libresAntes + 4,
                "eliminar no libera los bloques del archivo");
        sa.eliminar("/usuarios/foto.png");
        sa.eliminar("/usuarios");
        verificar(sa.getTotalEntradas() == 1 && disco.contarLibres(AreaDisco.DATOS) == 80,
                "Quedaron entradas o bloques despues de eliminar todo");
        esperarInvalida(() -> sa.eliminar("/"), "Elimina la raiz");
        esperarInvalida(() -> sa.eliminar("/usuarios"), "Elimina una ruta inexistente");
    }

    private static void probarEliminacionRecursiva() {
        Disco disco = new Disco(100, 20, 0);
        SistemaArchivos sa = new SistemaArchivos(disco);
        sa.crearDirectorio("/home", "admin", PermisoArchivo.PUBLICO);
        sa.crearDirectorio("/home/luciano", "luciano", PermisoArchivo.PRIVADO);
        sa.crearDirectorio("/home/luciano/fotos", "luciano", PermisoArchivo.PRIVADO);
        sa.crearDirectorio("/home/luciano/vacio", "luciano", PermisoArchivo.PRIVADO);
        sa.crearArchivo("/home/luciano/tarea.pdf", "luciano", 3, PermisoArchivo.PRIVADO, 1);
        sa.crearArchivo("/home/luciano/fotos/a.png", "luciano", 5, PermisoArchivo.PUBLICO, 2);
        sa.crearArchivo("/home/luciano/fotos/b.png", "luciano", 2, PermisoArchivo.PUBLICO, 3);
        sa.crearArchivo("/home/readme.txt", "admin", 4, PermisoArchivo.PUBLICO, 4);
        verificar(disco.contarLibres(AreaDisco.DATOS) == 66, "Los archivos no ocuparon 14 bloques");

        verificar(sa.eliminar("/home/luciano/fotos/b.png") == 2, "eliminar no informa los bloques del archivo");
        verificar(sa.eliminar("/home/luciano/vacio") == 0, "Un directorio vacio libero bloques");
        verificar(sa.eliminar("/home/luciano") == 8, "El borrado recursivo no libero 3 + 5 bloques");
        verificar(!sa.existe("/home/luciano") && !sa.existe("/home/luciano/fotos/a.png"),
                "Quedaron descendientes del directorio eliminado");
        verificar(sa.existe("/home/readme.txt") && iguales(sa.leer("/home/readme.txt"), 10, 11, 12, 13),
                "El borrado recursivo toco un archivo hermano");
        verificar(sa.getTotalEntradas() == 3 && disco.contarLibres(AreaDisco.DATOS) == 76,
                "El arbol o el disco no quedaron consistentes");
        for (VistaBloqueDisco bloque : disco.consultarBloques())
            verificar(bloque.libre() || bloque.numero() >= 10 && bloque.numero() <= 13,
                    "Quedo ocupado el bloque " + bloque.numero());
    }

    private static void probarPermisos() {
        Disco disco = new Disco(100, 20, 0);
        SistemaArchivos sa = new SistemaArchivos(disco);
        verificar(sa.getModo() == ModoUsuario.ADMINISTRADOR && sa.getUsuario().equals("admin"),
                "La sesion inicial no es de administrador");
        sa.crearDirectorio("/sistema", "admin", PermisoArchivo.PUBLICO);
        sa.crearArchivo("/sistema/boot.bin", "admin", 1, PermisoArchivo.PUBLICO, 0);
        sa.crearArchivo("/sistema/clave.cfg", "admin", 1, PermisoArchivo.PRIVADO, 0);
        sa.crearDirectorio("/home", "admin", PermisoArchivo.PUBLICO);
        sa.crearDirectorio("/home/user1", "user1", PermisoArchivo.PRIVADO);
        sa.crearDirectorio("/home/user2", "user2", PermisoArchivo.PUBLICO);
        sa.crearArchivo("/home/user2/privado.txt", "user2", 2, PermisoArchivo.PRIVADO, 1);
        sa.crearArchivo("/home/user2/publico.txt", "user2", 1, PermisoArchivo.PUBLICO, 1);

        sa.cambiarSesion(ModoUsuario.USUARIO, "user1");
        verificar(sa.getModo() == ModoUsuario.USUARIO && sa.getUsuario().equals("user1"), "No cambia la sesion");
        sa.crearArchivo("/home/user1/notas.txt", "user1", 2, PermisoArchivo.PRIVADO, 5);
        verificar(sa.existe("/home/user1/notas.txt"), "Un usuario no crea en su propio directorio");

        int libres = disco.contarLibres(AreaDisco.DATOS);
        int entradas = sa.getTotalEntradas();
        esperarRechazo(() -> sa.crearArchivo("/home/user2/x.txt", "user1", 1, PermisoArchivo.PUBLICO, 5),
                "Crea en el directorio de otro usuario");
        esperarRechazo(() -> sa.crearArchivo("/sistema/x.txt", "user1", 1, PermisoArchivo.PUBLICO, 5),
                "Crea en un directorio del sistema");
        esperarRechazo(() -> sa.crearArchivo("/home/user1/y.txt", "user2", 1, PermisoArchivo.PUBLICO, 5),
                "Crea a nombre de otro usuario");
        esperarRechazo(() -> sa.crearDirectorio("/home/user1/fotos", "user1", PermisoArchivo.PRIVADO),
                "Un usuario crea directorios");
        esperarRechazo(() -> sa.renombrar("/home/user1/notas.txt", "otra.txt"), "Un usuario renombra");
        esperarRechazo(() -> sa.eliminar("/home/user2/publico.txt"), "Elimina un archivo ajeno");
        esperarRechazo(() -> sa.eliminar("/sistema/boot.bin"), "Elimina un archivo del sistema");
        esperarRechazo(() -> sa.eliminar("/home/user1"), "Un usuario elimina un directorio");
        verificar(disco.contarLibres(AreaDisco.DATOS) == libres && sa.getTotalEntradas() == entradas
                && sa.existe("/home/user1/notas.txt"), "Una operacion rechazada cambio el sistema");

        verificar(sa.leer("/home/user1/notas.txt").length == 2, "No lee su archivo privado");
        verificar(sa.leer("/home/user2/publico.txt").length == 1 && sa.leer("/sistema/boot.bin").length == 1,
                "No lee archivos publicos");
        verificar(!sa.puedeLeer("/home/user2/privado.txt") && sa.puedeLeer("/home/user2/publico.txt"),
                "puedeLeer no respeta el permiso");
        esperarRechazo(() -> sa.leer("/home/user2/privado.txt"), "Lee un archivo privado ajeno");
        esperarRechazo(() -> sa.leer("/sistema/clave.cfg"), "Lee un archivo privado del sistema");
        verificar(sa.listar("/home/user2").length == 2, "Un usuario no ve la estructura completa");

        verificar(sa.eliminar("/home/user1/notas.txt") == 2 && disco.contarLibres(AreaDisco.DATOS) == libres + 2,
                "Un usuario no elimina su propio archivo");
        esperarInvalida(() -> sa.cambiarSesion(ModoUsuario.USUARIO, "admin"), "Un usuario actua como admin");
        esperarInvalida(() -> sa.cambiarSesion(ModoUsuario.USUARIO, " "), "Acepta un usuario vacio");
        esperarInvalida(() -> sa.cambiarSesion(null, "user1"), "Acepta un modo nulo");
        verificar(sa.getUsuario().equals("user1"), "Un cambio de sesion invalido cambio la sesion");

        sa.cambiarSesion(ModoUsuario.ADMINISTRADOR, "cualquiera");
        verificar(sa.getUsuario().equals("admin"), "El administrador no actua como admin");
        verificar(sa.leer("/home/user2/privado.txt").length == 2, "El administrador no lee todo");
        sa.renombrar("/home/user2/privado.txt", "secreto.txt");
        verificar(sa.eliminar("/home/user2") == 3, "El administrador no elimina directorios ajenos");
    }

    private static void probarFaltaDeEspacioYFragmentacion() {
        Disco disco = new Disco(100, 20, 0);
        SistemaArchivos sa = new SistemaArchivos(disco);
        verificar(sa.crearArchivo("/a.bin", "admin", 3, PermisoArchivo.PUBLICO, 1) == 0, "a.bin no empieza en 0");
        verificar(sa.crearArchivo("/b.bin", "admin", 2, PermisoArchivo.PUBLICO, 1) == 3, "b.bin no empieza en 3");
        verificar(sa.crearArchivo("/c.bin", "admin", 3, PermisoArchivo.PUBLICO, 1) == 5, "c.bin no empieza en 5");
        sa.eliminar("/b.bin");

        verificar(sa.crearArchivo("/d.bin", "admin", 4, PermisoArchivo.PUBLICO, 2) == 3,
                "El primer ajuste no reutiliza el hueco");
        verificar(iguales(sa.leer("/d.bin"), 3, 4, 8, 9), "d.bin no se encadena a traves del hueco");
        VistaBloqueDisco[] bloques = disco.consultarBloques();
        verificar(bloques[4].siguiente() == 8 && bloques[9].siguiente() == -1,
                "Los enlaces del archivo fragmentado no saltan sobre c.bin");
        verificar(iguales(sa.leer("/c.bin"), 5, 6, 7), "La fragmentacion toco a c.bin");

        int libres = disco.contarLibres(AreaDisco.DATOS);
        verificar(libres == 70, "No quedaron 70 bloques de datos libres");
        sa.crearArchivo("/lleno.bin", "admin", libres, PermisoArchivo.PUBLICO, 3);
        verificar(disco.contarLibres(AreaDisco.DATOS) == 0 && disco.contarLibres(AreaDisco.SWAP) == 20,
                "Un archivo ocupo el area swap");
        int entradas = sa.getTotalEntradas();
        try {
            sa.crearArchivo("/extra.bin", "admin", 1, PermisoArchivo.PUBLICO, 4);
            throw new AssertionError("Crea un archivo con el disco lleno");
        } catch (IllegalStateException esperado) {
            verificar(!sa.existe("/extra.bin") && sa.getTotalEntradas() == entradas,
                    "El disco lleno dejo una entrada");
        }

        sa.eliminar("/a.bin");
        try {
            sa.crearArchivo("/e.bin", "admin", 4, PermisoArchivo.PUBLICO, 5);
            throw new AssertionError("Crea 4 bloques donde solo hay 3");
        } catch (IllegalStateException esperado) {
            verificar(disco.contarLibres(AreaDisco.DATOS) == 3, "Un intento sin espacio ocupo bloques");
        }
        verificar(sa.crearArchivo("/e.bin", "admin", 3, PermisoArchivo.PUBLICO, 5) == 0
                && iguales(sa.leer("/e.bin"), 0, 1, 2), "No reutiliza los bloques que libero a.bin");
    }

    private static void probarVistas() {
        Disco disco = new Disco(100, 20, 0);
        SistemaArchivos sa = new SistemaArchivos(disco);
        sa.crearDirectorio("/home", "admin", PermisoArchivo.PUBLICO);
        sa.crearDirectorio("/home/user1", "user1", PermisoArchivo.PRIVADO);
        sa.crearArchivo("/home/user1/tarea.pdf", "user1", 3, PermisoArchivo.PRIVADO, 7);
        sa.crearArchivo("/leeme.txt", "admin", 1, PermisoArchivo.PUBLICO, 0);

        VistaEntradaArchivo raiz = sa.consultarArbol();
        verificar(raiz.ruta().equals("/") && raiz.toString().equals("/") && raiz.hijos().length == 2,
                "La raiz de la vista no tiene sus dos hijos");
        VistaEntradaArchivo tarea = raiz.hijos()[0].hijos()[0].hijos()[0];
        verificar(tarea.ruta().equals("/home/user1/tarea.pdf") && tarea.tamanoBloques() == 3
                && tarea.dueno().equals("user1") && tarea.permiso() == PermisoArchivo.PRIVADO
                && tarea.primerBloque() == 0 && !tarea.esDirectorio(),
                "El nodo del JTree no muestra nombre, tamano, dueno, permiso y primer bloque");
        VistaEntradaArchivo[] hijos = raiz.hijos();
        hijos[0] = null;
        verificar(raiz.hijos()[0] != null, "Se puede modificar la vista desde afuera");

        VistaAsignacionArchivo[] filas = sa.consultarAsignacion();
        verificar(filas.length == 2 && filas[0].ruta().equals("/home/user1/tarea.pdf")
                && filas[0].bloques() == 3 && filas[0].primerBloque() == 0 && filas[0].dueno().equals("user1")
                && filas[0].procesoCreador() == 7 && filas[1].nombre().equals("leeme.txt"),
                "La tabla de asignacion no coincide con los archivos");
        String etiqueta = filas[0].etiquetaDisco();
        verificar(disco.consultarBloques()[0].propietario().equals(etiqueta),
                "La etiqueta de la tabla no coincide con la del disco");

        sa.renombrar("/home/user1/tarea.pdf", "entrega.pdf");
        VistaAsignacionArchivo renombrada = sa.consultarAsignacion()[0];
        verificar(renombrada.nombre().equals("entrega.pdf") && renombrada.ruta().equals("/home/user1/entrega.pdf")
                && renombrada.etiquetaDisco().equals(etiqueta), "Renombrar cambio la etiqueta del archivo");
        verificar(tarea.nombre().equals("tarea.pdf"), "Una vista vieja cambio al renombrar");
    }

    static void esperarRechazo(Runnable accion, String mensaje) {
        try {
            accion.run();
            throw new AssertionError(mensaje);
        } catch (SecurityException esperado) {
            verificar(esperado.getMessage() != null && !esperado.getMessage().isBlank(),
                    "El rechazo no explica el motivo: " + mensaje);
        }
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
