package Pruebas;

import Modelo.PoliticaDisco;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Casos de aceptacion del disco aislado indicados por el enunciado. @author Adolfo */
public final class PruebaDiscoPlanificacion {
    private PruebaDiscoPlanificacion() { }

    public static void main(String[] args) throws Exception {
        probarBloquesCabezalYSwap();
        probarContratoPolitica();
        probarValidacionPoliticasBasicas();
        probarCasosDeProcesos();
        probarRecorridosDeBarrido();
        probarAnexoConDesplazamientos();
        System.out.println("OK: bloques, swap y casos FIFO, SSTF, SCAN y C-SCAN del enunciado");
    }

    private static void probarRecorridosDeBarrido() throws Exception {
        verificarBarrido("SCAN", "ASCENDENTE", new int[] {82, 17, 43, 140, 24, 16, 190},
                new int[] {82, 140, 190, 43, 24, 17, 16},
                new int[] {82, 140, 190, 199, 43, 24, 17, 16}, 3, -1, 332, "DESCENDENTE");
        verificarBarrido("CSCAN", "ASCENDENTE", new int[] {82, 17, 43, 140, 24, 16, 190},
                new int[] {82, 140, 190, 16, 17, 24, 43},
                new int[] {82, 140, 190, 199, 0, 16, 17, 24, 43}, 3, 4, 192, "ASCENDENTE");
        verificarBarrido("SCAN", "DESCENDENTE", new int[] {20, 80},
                new int[] {20, 80}, new int[] {20, 0, 80}, 1, -1, 130, "ASCENDENTE");
        verificarBarrido("CSCAN", "DESCENDENTE", new int[] {20, 80},
                new int[] {20, 80}, new int[] {20, 0, 199, 80}, 1, 2, 169, "DESCENDENTE");
        verificarBarrido("SCAN", "ASCENDENTE", new int[] {50, 50, 60, 40},
                new int[] {50, 50, 60, 40}, new int[] {50, 50, 60, 199, 40},
                3, -1, 308, "DESCENDENTE");
        verificarBarrido("CSCAN", "ASCENDENTE", new int[] {50, 50, 60, 40},
                new int[] {50, 50, 60, 40}, new int[] {50, 50, 60, 199, 0, 40},
                3, 4, 189, "ASCENDENTE");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void verificarBarrido(String clase, String direccionInicial, int[] solicitudes,
            int[] ordenEsperado, int[] destinos, int extremo, int salto,
            long desplazamiento, String direccionFinal) throws Exception {
        Class<?> direccionTipo = Class.forName("Modelo.DireccionDisco");
        Object direccion = Enum.valueOf((Class<? extends Enum>) direccionTipo, direccionInicial);
        Object politica = Class.forName("PlanificacionDisco." + clase).getConstructor().newInstance();
        Method planificar = politica.getClass().getMethod("planificar", int.class,
                int.class, direccionTipo, int[].class);
        int[] entradaOriginal = solicitudes.clone();
        Object resultado = planificar.invoke(politica, 50, 200, direccion, solicitudes);
        verificarArreglo(solicitudes, entradaOriginal, "El barrido modifica la cola recibida");
        verificarArreglo((int[]) invocar(resultado, "orden"), ordenEsperado,
                "Orden de barrido incorrecto para " + clase);
        Object[] movimientos = (Object[]) invocar(resultado, "movimientos");
        verificar(movimientos.length == destinos.length, "Cantidad de movimientos incorrecta para " + clase);
        for (int i = 0; i < movimientos.length; i++) {
            verificar((int) invocar(movimientos[i], "destino") == destinos[i],
                    "Destino incorrecto para " + clase + " en " + i);
            boolean atiende = (boolean) invocar(movimientos[i], "atiendeSolicitud");
            boolean contabiliza = (boolean) invocar(movimientos[i], "contabilizaDesplazamiento");
            verificar(atiende == (i != extremo && i != salto), "Atencion incorrecta para " + clase);
            verificar(contabiliza == (i != salto), "Metrica incorrecta para " + clase);
        }
        verificar((long) invocar(resultado, "desplazamientoTotal") == desplazamiento,
                "Desplazamiento de recorrido incorrecto para " + clase);
        verificar(invocar(resultado, "direccionFinal").toString().equals(direccionFinal),
                "Direccion final de recorrido incorrecta para " + clase);
    }

    private static void probarValidacionPoliticasBasicas() throws Exception {
        Class<?> direccionTipo = Class.forName("Modelo.DireccionDisco");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object ascendente = Enum.valueOf((Class<? extends Enum>) direccionTipo, "ASCENDENTE");
        for (String clase : new String[] {"PlanificacionDisco.FIFO", "PlanificacionDisco.SSTF",
                "PlanificacionDisco.SCAN", "PlanificacionDisco.CSCAN"}) {
            Object politica = Class.forName(clase).getConstructor().newInstance();
            Method planificar = politica.getClass().getMethod("planificar", int.class,
                    int.class, direccionTipo, int[].class);
            Object vacio = planificar.invoke(politica, 0, 200, ascendente, new int[0]);
            verificar(((int[]) invocar(vacio, "orden")).length == 0
                    && (long) invocar(vacio, "desplazamientoTotal") == 0L
                    && invocar(vacio, "direccionFinal") == ascendente,
                    "La cola vacia no conserva un resultado neutro");
            esperarPlanInvalido(planificar, politica, -1, 200, ascendente, new int[] {1});
            esperarPlanInvalido(planificar, politica, 200, 200, ascendente, new int[] {1});
            esperarPlanInvalido(planificar, politica, 0, 0, ascendente, new int[] {1});
            esperarPlanInvalido(planificar, politica, 0, 200, ascendente, new int[] {-1});
            esperarPlanInvalido(planificar, politica, 0, 200, ascendente, new int[] {200});
            esperarPlanInvalido(planificar, politica, 0, 200, null, new int[] {1});
            esperarPlanInvalido(planificar, politica, 0, 200, ascendente, null);
        }
    }

    private static void esperarPlanInvalido(Method metodo, Object politica, int cabeza,
            int total, Object direccion, int[] solicitudes) throws Exception {
        try {
            metodo.invoke(politica, cabeza, total, direccion, solicitudes);
            throw new AssertionError("Se aceptaron parametros invalidos de planificacion");
        } catch (InvocationTargetException esperado) {
            verificar(esperado.getCause() instanceof IllegalArgumentException,
                    "La politica falla con una excepcion incorrecta");
        }
    }

    private static void probarContratoPolitica() throws Exception {
        Class<?> contrato = Class.forName("PlanificacionDisco.PoliticaPlanificacionDisco");
        verificar(contrato.isInterface(), "La politica de disco debe ser una interfaz");
        Method tipo = contrato.getMethod("getTipo");
        Method planificar = contrato.getMethod("planificar", int.class, int.class,
                Class.forName("Modelo.DireccionDisco"), int[].class);
        verificar(tipo.getReturnType() == PoliticaDisco.class
                && !java.lang.reflect.Modifier.isStatic(tipo.getModifiers()), "getTipo no respeta el contrato");

        Class<?> resultadoTipo = Class.forName("Modelo.ResultadoPlanificacionDisco");
        verificar(planificar.getReturnType() == resultadoTipo
                && !java.lang.reflect.Modifier.isStatic(planificar.getModifiers()), "planificar no respeta el contrato");
        Class<?> movimientoTipo = Class.forName("Modelo.MovimientoCabezalDisco");
        Class<?> direccionTipo = Class.forName("Modelo.DireccionDisco");
        int[] original = {4, 8};
        Object[] movimientos = (Object[]) java.lang.reflect.Array.newInstance(movimientoTipo, 2);
        movimientos[0] = movimientoTipo.getConstructor(int.class, boolean.class, boolean.class)
                .newInstance(4, true, true);
        movimientos[1] = movimientoTipo.getConstructor(int.class, boolean.class, boolean.class)
                .newInstance(8, true, true);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object ascendente = Enum.valueOf((Class<? extends Enum>) direccionTipo, "ASCENDENTE");
        Object resultado = resultadoTipo.getConstructor(int[].class, movimientos.getClass(), long.class, direccionTipo)
                .newInstance(original, movimientos, 4L, ascendente);
        original[0] = 99;
        movimientos[0] = null;
        int[] primeraVista = (int[]) invocar(resultado, "orden");
        primeraVista[1] = 99;
        int[] segundaVista = (int[]) invocar(resultado, "orden");
        verificar(segundaVista[0] == 4 && segundaVista[1] == 8,
                "El resultado expone su arreglo mutable");
        Object[] primeraRuta = (Object[]) invocar(resultado, "movimientos");
        primeraRuta[1] = null;
        Object[] segundaRuta = (Object[]) invocar(resultado, "movimientos");
        verificar(segundaRuta[0] != null && segundaRuta[1] != null,
                "El resultado expone el arreglo de movimientos");
        verificar((long) invocar(resultado, "desplazamientoTotal") == 4L,
                "El resultado no conserva el desplazamiento");
        verificar(invocar(resultado, "direccionFinal") == ascendente,
                "El resultado no conserva la direccion final");
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
        bloques[0] = null;
        Object[] segundaLectura = (Object[]) discoTipo.getMethod("consultarBloques").invoke(disco);
        verificar(segundaLectura[0] != null, "La vista permite modificar el mapa interno");

        esperarConstructorInvalido(discoTipo, 99, 10, 50);
        esperarConstructorInvalido(discoTipo, 501, 10, 50);
        esperarConstructorInvalido(discoTipo, 200, 0, 50);
        esperarConstructorInvalido(discoTipo, 200, 200, 50);
        esperarConstructorInvalido(discoTipo, 200, 50, -1);
        esperarConstructorInvalido(discoTipo, 200, 50, 200);

        verificarConfiguracionLimite(discoTipo, 100, 1, 0, 99);
        verificarConfiguracionLimite(discoTipo, 500, 499, 499, 1);
    }

    private static void verificarConfiguracionLimite(Class<?> discoTipo, int total, int swap,
            int cabeza, int inicioSwap) throws Exception {
        Object disco = discoTipo.getConstructor(int.class, int.class, int.class).newInstance(total, swap, cabeza);
        verificar((int) discoTipo.getMethod("getTotalBloques").invoke(disco) == total, "Total no configurable");
        verificar((int) discoTipo.getMethod("getTamanoSwap").invoke(disco) == swap, "Swap no configurable");
        verificar((int) discoTipo.getMethod("getPosicionCabezal").invoke(disco) == cabeza, "Cabezal no configurable");
        Object[] bloques = (Object[]) discoTipo.getMethod("consultarBloques").invoke(disco);
        for (int i = 0; i < bloques.length; i++)
            verificar(invocar(bloques[i], "area").toString().equals(i < inicioSwap ? "DATOS" : "SWAP"),
                    "Frontera configurable de swap incorrecta");
    }

    private static void probarCasosDeProcesos() throws Exception {
        int[] solicitudes = {95, 180, 34, 119, 11, 123, 62, 64};
        verificarPlan(PoliticaDisco.FIFO, 50, solicitudes, new int[] {95, 180, 34, 119, 11, 123, 62, 64}, -1);
        verificarPlan(PoliticaDisco.SSTF, 50, solicitudes, new int[] {62, 64, 34, 11, 95, 119, 123, 180}, -1);
        verificarPlan(PoliticaDisco.FIFO, 50, new int[] {40}, new int[] {40}, 10);
        verificarPlan(PoliticaDisco.SSTF, 50, new int[] {40, 60, 40, 60},
                new int[] {40, 40, 60, 60}, 30);
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
        String clase = switch (politica) {
            case FIFO -> "FIFO";
            case SSTF -> "SSTF";
            case SCAN -> "SCAN";
            case C_SCAN -> "CSCAN";
        };
        Object estrategia = Class.forName("PlanificacionDisco." + clase).getConstructor().newInstance();
        verificar(estrategia.getClass().getMethod("getTipo").invoke(estrategia) == politica,
                "La estrategia informa un tipo incorrecto");
        Method planificar = estrategia.getClass().getMethod("planificar", int.class,
                int.class, direccionTipo, int[].class);
        int[] entradaOriginal = solicitudes.clone();
        Object resultado = planificar.invoke(estrategia, cabeza, 200, ascendente, solicitudes);
        verificarArreglo(solicitudes, entradaOriginal, "La politica modifica la cola recibida");
        int[] orden = (int[]) invocar(resultado, "orden");
        verificarArreglo(orden, ordenEsperado, "Orden incorrecto para " + politica);
        Object[] movimientos = (Object[]) invocar(resultado, "movimientos");
        if (politica == PoliticaDisco.FIFO || politica == PoliticaDisco.SSTF) {
            verificar(movimientos.length == orden.length, "Recorrido incompleto para " + politica);
            int posicion = cabeza;
            String direccionEsperada = "ASCENDENTE";
            for (int i = 0; i < movimientos.length; i++) {
                verificar((int) invocar(movimientos[i], "destino") == orden[i]
                        && (boolean) invocar(movimientos[i], "contabilizaDesplazamiento")
                        && (boolean) invocar(movimientos[i], "atiendeSolicitud"),
                        "Movimiento directo incorrecto para " + politica);
                if (orden[i] > posicion) direccionEsperada = "ASCENDENTE";
                else if (orden[i] < posicion) direccionEsperada = "DESCENDENTE";
                posicion = orden[i];
            }
            verificar(invocar(resultado, "direccionFinal").toString().equals(direccionEsperada),
                    "Direccion final incorrecta para " + politica);
        }
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
