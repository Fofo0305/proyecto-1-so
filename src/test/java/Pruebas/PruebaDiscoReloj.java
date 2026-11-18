package Pruebas;

import Modelo.OperacionES;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Contrato de la cola unica y del avance del disco por ciclos. @author Adolfo */
public final class PruebaDiscoReloj {
    private PruebaDiscoReloj() { }

    public static void main(String[] args) throws Exception {
        Class<?> discoTipo = Class.forName("Sistema.Disco");
        Object disco = discoTipo.getConstructor(int.class, int.class, int.class, int.class)
                .newInstance(200, 50, 50, 10);
        Method encolar = discoTipo.getMethod("encolarSolicitud", long.class,
                OperacionES.class, int.class, int.class);
        long primera = (long) encolar.invoke(disco, 7L, OperacionES.LEER, 70, 1);
        long segunda = (long) encolar.invoke(disco, 8L, OperacionES.LEER, 30, 1);
        verificar(primera != segunda, "Las solicitudes no reciben identificadores unicos");
        verificar(((Object[]) discoTipo.getMethod("consultarCola").invoke(disco)).length == 2,
                "La cola unica no conserva las solicitudes");

        Method avanzar = discoTipo.getMethod("avanzarCiclo", long.class);
        avanzar.invoke(disco, 1L);
        verificar(cabezal(discoTipo, disco) == 60, "El cabezal no respeta posiciones por ciclo");
        avanzar.invoke(disco, 2L);
        verificar(cabezal(discoTipo, disco) == 70, "El cabezal no alcanza la primera solicitud");
        verificar(discoTipo.getMethod("consultarActiva").invoke(disco) != null,
                "La solicitud no queda activa al llegar al bloque");
        avanzar.invoke(disco, 3L);
        Object[] completadas = (Object[]) discoTipo.getMethod("consultarCompletadas").invoke(disco);
        verificar(completadas.length == 1 && (long) invocar(completadas[0], "id") == primera,
                "El disco no completa una solicitud por vez");

        Object sstf = Class.forName("PlanificacionDisco.SSTF").getConstructor().newInstance();
        discoTipo.getMethod("cambiarPolitica", Class.forName("PlanificacionDisco.PoliticaPlanificacionDisco"))
                .invoke(disco, sstf);
        long tercera = (long) encolar.invoke(disco, 9L, OperacionES.LEER, 90, 1);
        avanzar.invoke(disco, 4L);
        avanzar.invoke(disco, 5L);
        avanzar.invoke(disco, 6L);
        completadas = (Object[]) discoTipo.getMethod("consultarCompletadas").invoke(disco);
        verificar(completadas.length == 2 && (long) invocar(completadas[1], "id") == tercera,
                "El cambio de politica no reordena la cola pendiente");
        verificar(((Object[]) discoTipo.getMethod("consultarCola").invoke(disco)).length == 1,
                "El disco atiende mas de una solicitud simultaneamente");

        long cuarta = (long) encolar.invoke(disco, 10L, OperacionES.LEER, 90, 2);
        avanzar.invoke(disco, 7L);
        Object activa = discoTipo.getMethod("consultarActiva").invoke(disco);
        verificar(activa != null && (long) invocar(activa, "id") == cuarta
                && (int) invocar(activa, "bloquesRestantes") == 1,
                "Cada bloque no consume exactamente un ciclo de trabajo");
        avanzar.invoke(disco, 8L);
        completadas = (Object[]) discoTipo.getMethod("consultarCompletadas").invoke(disco);
        verificar(completadas.length == 3 && (long) invocar(completadas[2], "id") == cuarta,
                "La solicitud de varios bloques termina fuera de tiempo");

        try {
            avanzar.invoke(disco, 8L);
            throw new AssertionError("El disco acepto un ciclo repetido");
        } catch (InvocationTargetException esperado) {
            verificar(esperado.getCause() instanceof IllegalArgumentException,
                    "El ciclo repetido falla con excepcion incorrecta");
        }
        esperarSolicitudInvalida(encolar, disco, 10L, OperacionES.LEER, 150, 1);
        esperarSolicitudInvalida(encolar, disco, 11L, OperacionES.SUSPENDER, 149, 1);
        esperarSolicitudInvalida(encolar, disco, 12L, OperacionES.REACTIVAR, 149, 1);
        esperarSolicitudInvalida(encolar, disco, 13L, OperacionES.LEER, 1, 0);
        encolar.invoke(disco, 14L, OperacionES.SUSPENDER, 150, 1);
        encolar.invoke(disco, 15L, OperacionES.REACTIVAR, 151, 1);
        verificar(((Object[]) discoTipo.getMethod("consultarCola").invoke(disco)).length == 3,
                "Las operaciones de swap validas fueron rechazadas");

        Object ciclos = discoTipo.getConstructor(int.class, int.class, int.class, int.class)
                .newInstance(200, 50, 50, 10);
        try {
            avanzar.invoke(ciclos, 2L);
            throw new AssertionError("El disco acepto un salto de ciclo");
        } catch (InvocationTargetException esperado) {
            verificar(esperado.getCause() instanceof IllegalArgumentException,
                    "El salto de ciclo falla con excepcion incorrecta");
        }
        verificar((long) discoTipo.getMethod("getUltimoCiclo").invoke(ciclos) == 0L
                && cabezal(discoTipo, ciclos) == 50, "El ciclo rechazado modifico el disco");
        avanzar.invoke(ciclos, 1L);
        verificar((long) discoTipo.getMethod("getUltimoCiclo").invoke(ciclos) == 1L,
                "El disco no se recupera tras rechazar un salto");
        System.out.println("OK: cola unica, politica intercambiable y avance del disco por reloj");
    }

    private static int cabezal(Class<?> tipo, Object disco) throws Exception {
        return (int) tipo.getMethod("getPosicionCabezal").invoke(disco);
    }

    private static Object invocar(Object objetivo, String metodo) throws Exception {
        return objetivo.getClass().getMethod(metodo).invoke(objetivo);
    }

    private static void esperarSolicitudInvalida(Method metodo, Object disco, long proceso,
            OperacionES operacion, int bloque, int trabajo) throws Exception {
        try {
            metodo.invoke(disco, proceso, operacion, bloque, trabajo);
            throw new AssertionError("Se acepto una solicitud de disco invalida");
        } catch (InvocationTargetException esperado) {
            verificar(esperado.getCause() instanceof IllegalArgumentException,
                    "La solicitud invalida falla con excepcion incorrecta");
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
