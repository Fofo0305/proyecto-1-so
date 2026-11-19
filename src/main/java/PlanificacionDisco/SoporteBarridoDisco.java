package PlanificacionDisco;

/** Operaciones de arreglos compartidas por SCAN y C-SCAN. @author Adolfo */
final class SoporteBarridoDisco {
    private SoporteBarridoDisco() { }

    static int[] lado(int[] solicitudes, int cabezal, boolean superiores,
            boolean incluirCabezal, boolean ascendente) {
        int cantidad = 0;
        for (int solicitud : solicitudes)
            if (pertenece(solicitud, cabezal, superiores, incluirCabezal)) cantidad++;
        int[] lado = new int[cantidad];
        int indice = 0;
        for (int solicitud : solicitudes)
            if (pertenece(solicitud, cabezal, superiores, incluirCabezal)) lado[indice++] = solicitud;
        ordenar(lado, ascendente);
        return lado;
    }

    private static boolean pertenece(int solicitud, int cabezal, boolean superiores, boolean incluirCabezal) {
        if (superiores) return incluirCabezal ? solicitud >= cabezal : solicitud > cabezal;
        return incluirCabezal ? solicitud <= cabezal : solicitud < cabezal;
    }

    static int[] combinar(int[] primero, int[] segundo) {
        int[] resultado = new int[primero.length + segundo.length];
        System.arraycopy(primero, 0, resultado, 0, primero.length);
        System.arraycopy(segundo, 0, resultado, primero.length, segundo.length);
        return resultado;
    }

    private static void ordenar(int[] datos, boolean ascendente) {
        for (int i = 1; i < datos.length; i++) {
            int valor = datos[i];
            int j = i - 1;
            while (j >= 0 && (ascendente ? datos[j] > valor : datos[j] < valor)) {
                datos[j + 1] = datos[j];
                j--;
            }
            datos[j + 1] = valor;
        }
    }
}
