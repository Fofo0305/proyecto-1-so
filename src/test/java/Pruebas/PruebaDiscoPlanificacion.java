package Pruebas;

import Modelo.PoliticaDisco;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Casos de aceptacion del disco aislado indicados por el enunciado. @author Adolfo */
public final class PruebaDiscoPlanificacion {
    private PruebaDiscoPlanificacion() { }

    public static void main(String[] args) throws Exception {
        probarBloquesCabezalYSwap();
        probarCasosDeProcesos();
        probarAnexoConDesplazamientos();
        System.out.println("OK: bloques, swap y casos FIFO, SSTF, SCAN y C-SCAN del enunciado");
    }

    private static void probarBloquesCabezalYSwap() throws Exception {
        Class<?> discoTipo = Class.forName("Sistema.Disco");
        Object disco = discoTipo.getConstructor(int.class, int.class, int.class).newInstance(200, 50, 50);
        verificar((int) discoTipo.getMethod("getTotalBloques").invoke(disco) == 200, "Cantidad de bloques incorrecta");
        verificar((int) discoTipo.getMethod("getTamanoSwap").invoke(disco) == 50, "Tamano de swap incorrecto");
        verificar((int) discoTipo.getMethod("getPosicionCabezal").invoke(disco) == 50, "Cabezal inicial incorrecto");
        verificar("ASCENDENTE".equals(discoTipo.getMethod("getDireccion").invoke(disco).toString()),
                "La direccion inicial debe apuntar a bloques mayores");

        Object[] bloques = (Object[]) discoTipo.getMethod("consultarBloques").invoke(disco);
        verificar(bloques.length == 200, "El mapa no representa todos los bloques");
        for (int i = 0; i < bloques.length; i++) {
            Object bloque = bloques[i];
            verificar((int) invocar(bloque, "numero") == i, "Numeracion de bloques discontinua");
            String area = invocar(bloque, "area").toString();
            verificar(area.equals(i < 150 ? "DATOS" : "SWAP"), "Limite de swap incorrecto en " + i);
            verificar(invocar(bloque, "propietario") == null, "Un bloque nuevo aparece ocupado");
            verificar((int) invocar(bloque, "siguiente") == -1, "Un bloque libre tiene enlace");
        }

        esperarConstructorInvalido(discoTipo, 99, 10, 50);
        esperarConstructorInvalido(discoTipo, 501, 10, 50);
        esperarConstructorInvalido(discoTipo, 200, 0, 50);
        esperarConstructorInvalido(discoTipo, 200, 200, 50);
        esperarConstructorInvalido(discoTipo, 200, 50, 200);
    }

    private static void probarCasosDeProcesos() throws Exception {
        int[] solicitudes = {95, 180, 34, 119, 11, 123, 62, 64};
        verificarPlan(PoliticaDisco.FIFO, 50, solicitudes, new int[] {95, 180, 34, 119, 11, 123, 62, 64}, -1);
        verificarPlan(PoliticaDisco.SSTF, 50, solicitudes, new int[] {62, 64, 34, 11, 95, 119, 123, 180}, -1);
        verificarPlan(PoliticaDisco.SCAN, 50, solicitudes, new int[] {62, 64, 95, 119, 123, 180, 34, 11}, -1);
        verificarPlan(PoliticaDisco.C_SCAN, 50, solicitudes, new int[] {62, 64, 95, 119, 123, 180, 11, 34}, -1);
    }

    private static void probarAnexoConDesplazamientos() throws Exception {
        int[] solicitudes = {82, 17, 43, 140, 24, 16, 190};
        verificarPlan(PoliticaDisco.FIFO, 50, solicitudes, new int[] {82, 17, 43, 140, 24, 16, 190}, 518);
        verificarPlan(PoliticaDisco.SSTF, 50, solicitudes, new int[] {43, 24, 17, 16, 82, 140, 190}, 208);
        verificarPlan(PoliticaDisco.SCAN, 50, solicitudes, new int[] {82, 140, 190, 43, 24, 17, 16}, 332);
        verificarPlan(PoliticaDisco.C_SCAN, 50, solicitudes, new int[] {82, 140, 190, 16, 17, 24, 43}, 192);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void verificarPlan(PoliticaDisco politica, int cabeza, int[] solicitudes,
            int[] ordenEsperado, int desplazamientoEsperado) throws Exception {
        Class<?> direccionTipo = Class.forName("Modelo.DireccionDisco");
        Object ascendente = Enum.valueOf((Class<? extends Enum>) direccionTipo, "ASCENDENTE");
        Class<?> planificadorTipo = Class.forName("PlanificacionDisco.PlanificadorDisco");
        Method planificar = planificadorTipo.getMethod("planificar", PoliticaDisco.class, int.class,
                int.class, direccionTipo, int[].class);
        Object resultado = planificar.invoke(null, politica, cabeza, 200, ascendente, solicitudes);
        int[] orden = (int[]) invocar(resultado, "orden");
        verificarArreglo(orden, ordenEsperado, "Orden incorrecto para " + politica);
        if (desplazamientoEsperado >= 0)
            verificar((long) invocar(resultado, "desplazamientoTotal") == desplazamientoEsperado,
                    "Desplazamiento incorrecto para " + politica);
    }

    private static Object invocar(Object objetivo, String metodo) throws Exception {
        return objetivo.getClass().getMethod(metodo).invoke(objetivo);
    }

    private static void esperarConstructorInvalido(Class<?> tipo, int bloques, int swap, int cabeza) throws Exception {
        try {
            tipo.getConstructor(int.class, int.class, int.class).newInstance(bloques, swap, cabeza);
            throw new AssertionError("Se acepto configuracion invalida: " + bloques + "/" + swap + "/" + cabeza);
        } catch (InvocationTargetException esperado) {
            verificar(esperado.getCause() instanceof IllegalArgumentException, "Fallo con excepcion incorrecta");
        }
    }

    private static void verificarArreglo(int[] actual, int[] esperado, String mensaje) {
        verificar(actual.length == esperado.length, mensaje + " (cantidad)");
        for (int i = 0; i < esperado.length; i++)
            verificar(actual[i] == esperado[i], mensaje + " en posicion " + i);
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
