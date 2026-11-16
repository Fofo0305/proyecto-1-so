package Pruebas;

import Modelo.DefinicionProceso;
import Modelo.EstadoProceso;
import Modelo.MotivoBloqueo;
import Modelo.OperacionES;
import Modelo.ParametrosES;
import Modelo.Proceso;
import Modelo.TipoProceso;
import Sistema.Computador;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Contrato del modelo de dominio que necesitara el disco y la suspension. @author Adolfo */
public final class PruebaModeloSieteEstados {
    private PruebaModeloSieteEstados() { }

    public static void main(String[] args) throws Exception {
        probarEnumsObligatorios();
        probarMatrizCompleta();
        probarTransicionesSuspendidas();
        probarOperacionDeDiscoEnPCB();
        probarContratoDefinicion();
        probarEstructurasPropias();
        System.out.println("OK: enums, PCB, siete estados y estructuras enlazadas propias");
    }

    private static void probarMatrizCompleta() {
        EstadoProceso[] estados = EstadoProceso.values();
        boolean[][] permitidas = {
            {false, true,  false, false, false, false, true},
            {false, false, true,  false, true,  false, true},
            {false, true,  false, true,  false, false, true},
            {false, true,  false, false, false, true,  true},
            {false, true,  false, false, false, false, true},
            {false, false, false, true,  true,  false, true},
            {false, false, false, false, false, false, false}
        };
        for (int origen = 0; origen < estados.length; origen++)
            for (int destino = 0; destino < estados.length; destino++)
                verificar(estados[origen].permiteTransicionA(estados[destino]) == permitidas[origen][destino],
                        "Matriz incorrecta: " + estados[origen] + " -> " + estados[destino]);

        Proceso sinMotivo = proceso("Sin motivo");
        sinMotivo.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
        sinMotivo.cambiarEstado(EstadoProceso.EJECUCION, MotivoBloqueo.NINGUNO);
        esperarArgumento(() -> sinMotivo.cambiarEstado(EstadoProceso.BLOQUEADO, MotivoBloqueo.NINGUNO),
                "BLOQUEADO sin motivo");

        Proceso suspendidoSinMotivo = proceso("Suspendido sin motivo");
        suspendidoSinMotivo.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
        suspendidoSinMotivo.cambiarEstado(EstadoProceso.EJECUCION, MotivoBloqueo.NINGUNO);
        suspendidoSinMotivo.cambiarEstado(EstadoProceso.BLOQUEADO, MotivoBloqueo.ENTRADA_SALIDA);
        esperarArgumento(() -> suspendidoSinMotivo.cambiarEstado(
                EstadoProceso.BLOQUEADO_SUSPENDIDO, MotivoBloqueo.NINGUNO), "BLOQUEADO_SUSPENDIDO sin motivo");

        Proceso motivoSobrante = proceso("Motivo sobrante");
        esperarArgumento(() -> motivoSobrante.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.ENTRADA_SALIDA),
                "estado no bloqueado con motivo");
    }

    private static void esperarArgumento(Runnable operacion, String caso) {
        try {
            operacion.run();
            throw new AssertionError("Se acepto " + caso);
        } catch (IllegalArgumentException esperado) { }
    }

    private static void probarEnumsObligatorios() throws Exception {
        enumExacto("Modelo.OperacionES", "CREAR", "LEER", "ACTUALIZAR", "ELIMINAR", "SUSPENDER", "REACTIVAR");
        enumExacto("Modelo.PoliticaDisco", "FIFO", "SSTF", "SCAN", "C_SCAN");
        enumExacto("Modelo.ModoUsuario", "ADMINISTRADOR", "USUARIO");
        enumExacto("Modelo.PermisoArchivo", "PUBLICO", "PRIVADO");
        EstadoProceso.valueOf("LISTO_SUSPENDIDO");
        EstadoProceso.valueOf("BLOQUEADO_SUSPENDIDO");
        verificar(EstadoProceso.values().length == 7, "El proceso no tiene exactamente siete estados");
    }

    private static void enumExacto(String nombre, String... esperados) throws Exception {
        Object[] valores = (Object[]) Class.forName(nombre).getMethod("values").invoke(null);
        verificar(valores.length == esperados.length, nombre + " tiene valores inesperados");
        for (int i = 0; i < esperados.length; i++)
            verificar(((Enum<?>) valores[i]).name().equals(esperados[i]), nombre + " no conserva el contrato");
    }

    private static void probarTransicionesSuspendidas() {
        EstadoProceso listoSuspendido = EstadoProceso.valueOf("LISTO_SUSPENDIDO");
        EstadoProceso bloqueadoSuspendido = EstadoProceso.valueOf("BLOQUEADO_SUSPENDIDO");

        Proceso listo = proceso("Listo");
        listo.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
        listo.cambiarEstado(listoSuspendido, MotivoBloqueo.NINGUNO);
        listo.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
        verificar(listo.getEstado() == EstadoProceso.LISTO, "Un listo suspendido no puede reactivarse");

        Proceso bloqueado = proceso("Bloqueado");
        bloqueado.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
        bloqueado.cambiarEstado(EstadoProceso.EJECUCION, MotivoBloqueo.NINGUNO);
        bloqueado.cambiarEstado(EstadoProceso.BLOQUEADO, MotivoBloqueo.ENTRADA_SALIDA);
        bloqueado.cambiarEstado(bloqueadoSuspendido, MotivoBloqueo.ENTRADA_SALIDA);
        bloqueado.cambiarEstado(listoSuspendido, MotivoBloqueo.NINGUNO);
        verificar(bloqueado.getMotivoBloqueo() == MotivoBloqueo.NINGUNO,
                "Al ocurrir el evento suspendido debe limpiarse el motivo");

        Proceso invalido = proceso("Invalido");
        try {
            invalido.cambiarEstado(bloqueadoSuspendido, MotivoBloqueo.ENTRADA_SALIDA);
            throw new AssertionError("Se permitio suspender como bloqueado a un proceso nuevo");
        } catch (IllegalStateException esperado) { }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void probarOperacionDeDiscoEnPCB() throws Exception {
        Class<?> tipoOperacion = Class.forName("Modelo.OperacionES");
        Object leer = Enum.valueOf((Class<? extends Enum>) tipoOperacion, "LEER");
        Proceso proceso = proceso("Disco");
        Method configurar = Proceso.class.getMethod("configurarOperacionDisco", tipoOperacion, String.class);
        configurar.invoke(proceso, leer, "/usuarios/notas.txt");
        verificar(Proceso.class.getMethod("getOperacionES").invoke(proceso) == leer,
                "El PCB no conserva la operacion de disco");
        verificar("/usuarios/notas.txt".equals(Proceso.class.getMethod("getRutaObjetivo").invoke(proceso)),
                "El PCB no conserva la ruta objetivo");
    }

    private static void probarContratoDefinicion() {
        DefinicionProceso definicion = new DefinicionProceso("Lectura", 1, 0, 4, 2, 20,
                OperacionES.LEER, "/usuarios/notas.txt");
        Proceso lectura = definicion.crearProceso();
        verificar(lectura.getOperacionES() == OperacionES.LEER
                && "/usuarios/notas.txt".equals(lectura.getRutaObjetivo()),
                "DefinicionProceso no transfiere la solicitud al PCB");

        esperarArgumento(() -> new DefinicionProceso("Relativa", 1, 0, 4, 2, 20,
                OperacionES.LEER, "usuarios/notas.txt"), "ruta relativa");
        esperarArgumento(() -> new DefinicionProceso("Dos mecanismos", 1, 0, 4, 2, 20,
                TipoProceso.IO_BOUND, new ParametrosES(1, 1), OperacionES.LEER, "/archivo"),
                "E/S temporal y de disco simultaneas");

        DefinicionProceso suspension = new DefinicionProceso("Suspension", 1, 0, 4, 2, 20,
                OperacionES.SUSPENDER, null);
        verificar(suspension.crearProceso().getRutaObjetivo() == null,
                "La suspension exige una ruta de archivo artificial");

        Proceso repetido = proceso("Repetido");
        repetido.configurarOperacionDisco(OperacionES.LEER, "/archivo");
        esperarEstado(() -> repetido.configurarOperacionDisco(OperacionES.LEER, "/otro"),
                "configurar la operacion dos veces");

        Proceso admitido = proceso("Admitido");
        admitido.cambiarEstado(EstadoProceso.LISTO, MotivoBloqueo.NINGUNO);
        esperarEstado(() -> admitido.configurarOperacionDisco(OperacionES.LEER, "/archivo"),
                "configurar la operacion despues de NUEVO");

        Computador computador = new Computador(1, 64);
        esperarArgumento(() -> computador.recibirProceso(proceso("Incompleto")),
                "admitir un IO_BOUND sin mecanismo de E/S");
        verificar(computador.consultarVista().colaNuevos().length == 0,
                "El computador registro un PCB incompleto");
    }

    private static void esperarEstado(Runnable operacion, String caso) {
        try {
            operacion.run();
            throw new AssertionError("Se permitio " + caso);
        } catch (IllegalStateException esperado) { }
    }

    private static void probarEstructurasPropias() throws Exception {
        Class<?> listaTipo = Class.forName("EDD.ListaEnlazada");
        Object lista = listaTipo.getConstructor().newInstance();
        listaTipo.getMethod("agregar", Object.class).invoke(lista, "a");
        listaTipo.getMethod("agregar", Object.class).invoke(lista, "b");
        verificar((int) listaTipo.getMethod("getTamano").invoke(lista) == 2, "La lista pierde elementos");
        verificar("b".equals(listaTipo.getMethod("obtener", int.class).invoke(lista, 1)), "La lista pierde el orden");
        verificar((boolean) listaTipo.getMethod("eliminar", Object.class).invoke(lista, "a"), "La lista no elimina");

        Class<?> arbolTipo = Class.forName("EDD.ArbolJerarquico");
        Class<?> nodoTipo = Class.forName("EDD.NodoArbol");
        Constructor<?> constructor = arbolTipo.getConstructor(Object.class);
        Object arbol = constructor.newInstance("/");
        Object raiz = arbolTipo.getMethod("getRaiz").invoke(arbol);
        Object usuarios = arbolTipo.getMethod("agregarHijo", nodoTipo, Object.class).invoke(arbol, raiz, "usuarios");
        arbolTipo.getMethod("agregarHijo", nodoTipo, Object.class).invoke(arbol, usuarios, "notas.txt");
        verificar((int) arbolTipo.getMethod("getTamano").invoke(arbol) == 3, "El arbol no cuenta la jerarquia");
        verificar((boolean) arbolTipo.getMethod("eliminarSubarbol", nodoTipo).invoke(arbol, usuarios),
                "El arbol no elimina un subarbol");
        verificar((int) arbolTipo.getMethod("getTamano").invoke(arbol) == 1, "El arbol no descuenta descendientes");
        try {
            arbolTipo.getMethod("eliminarSubarbol", nodoTipo).invoke(arbol, usuarios);
            throw new AssertionError("Un nodo separado continua perteneciendo al arbol");
        } catch (java.lang.reflect.InvocationTargetException esperado) {
            verificar(esperado.getCause() instanceof IllegalArgumentException,
                    "El arbol no distingue un nodo separado");
        }
    }

    private static Proceso proceso(String nombre) {
        return new Proceso(nombre, 1, TipoProceso.IO_BOUND, 0, 4, 4, 20);
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
