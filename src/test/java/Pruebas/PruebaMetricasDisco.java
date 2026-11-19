package Pruebas;

import Modelo.EventoSistema;
import Modelo.OperacionES;
import Modelo.PoliticaDisco;
import Modelo.VistaMetricasDisco;
import Modelo.VistaSolicitudDisco;
import PlanificacionDisco.CSCAN;
import PlanificacionDisco.SSTF;
import Sistema.Disco;

/** Metricas, historia y bitacora del disco local. @author Adolfo */
public final class PruebaMetricasDisco {
    private PruebaMetricasDisco() { }

    public static void main(String[] args) {
        Disco disco = new Disco(200, 50, 50, 10);
        disco.encolarSolicitud(1, OperacionES.LEER, 70, 1);
        disco.encolarSolicitud(2, OperacionES.LEER, 30, 1);
        for (long ciclo = 1; ciclo <= 8; ciclo++) disco.avanzarCiclo(ciclo);

        VistaMetricasDisco metricas = disco.consultarMetricas();
        verificar(metricas.ciclo() == 8 && metricas.desplazamientoTotal() == 60,
                "El desplazamiento acumulado no coincide con el cabezal");
        verificar(metricas.solicitudesAtendidas() == 2
                && metricas.atendidas(PoliticaDisco.FIFO) == 2
                && metricas.atendidas(PoliticaDisco.SSTF) == 0,
                "El conteo por politica es incorrecto");
        verificar(Math.abs(metricas.esperaPromedio() - 1.5) < 0.0001,
                "La espera promedio no usa el tiempo en cola");
        long[] ciclosEsperados = {1, 2, 3, 4, 5, 6, 7, 8};
        long[] desplazamientosEsperados = {10, 20, 20, 30, 40, 50, 60, 60};
        verificarArreglo(metricas.ciclos(), ciclosEsperados, "Eje temporal incorrecto");
        verificarArreglo(metricas.desplazamientoAcumulado(), desplazamientosEsperados,
                "Historia de desplazamiento incorrecta");
        long[] historia = metricas.desplazamientoAcumulado();
        long[] ciclos = metricas.ciclos();
        long[] porPolitica = metricas.atendidasPorPolitica();
        historia[7] = 999;
        ciclos[0] = 999;
        porPolitica[PoliticaDisco.FIFO.ordinal()] = 999;
        verificar(metricas.desplazamientoAcumulado()[7] == 60
                && metricas.ciclos()[0] == 1
                && metricas.atendidas(PoliticaDisco.FIFO) == 2,
                "La misma vista de metricas expone sus arreglos mutables");

        VistaSolicitudDisco[] completadas = disco.consultarCompletadas();
        verificar(completadas[0].politicaSeleccion() == PoliticaDisco.FIFO
                && completadas[0].tiempoEspera() == 0
                && completadas[1].tiempoEspera() == 3,
                "La solicitud no conserva politica y espera de seleccion");
        int llegadas = 0, decisiones = 0, finales = 0;
        for (EventoSistema evento : disco.consultarEventos()) {
            if (evento.tipo().equals("SOLICITUD_DISCO")) {
                llegadas++;
                verificar(evento.ciclo() == 0 && (evento.proceso() == 1 || evento.proceso() == 2),
                        "La llegada se registra con ciclo o proceso incorrecto");
            }
            if (evento.tipo().equals("DISCO_ATIENDE")) {
                decisiones++;
                verificar(evento.detalle().contains("FIFO"), "La decision no registra la politica");
                verificar(evento.ciclo() == (evento.proceso() == 1 ? 1 : 4),
                        "La decision se registra en un ciclo incorrecto");
            }
            if (evento.tipo().equals("DISCO_COMPLETA")) {
                finales++;
                verificar(evento.ciclo() == (evento.proceso() == 1 ? 3 : 8),
                        "La finalizacion se registra en un ciclo incorrecto");
            }
        }
        verificar(llegadas == 2 && decisiones == 2 && finales == 2,
                "La bitacora pierde llegadas, decisiones o finales");
        disco.encolarSolicitud(6, OperacionES.LEER, 30, 1);
        disco.avanzarCiclo(9);
        verificar(metricas.atendidas(PoliticaDisco.FIFO) == 2
                && disco.consultarMetricas().atendidas(PoliticaDisco.FIFO) == 3,
                "Una vista antigua cambia cuando el disco actualiza sus contadores");

        long[] conteosFuente = {2, 0, 0, 0};
        long[] ciclosFuente = {1};
        long[] historiaFuente = {10};
        VistaMetricasDisco vistaAislada = new VistaMetricasDisco(1, 10, 2, 0,
                0, 0, conteosFuente, ciclosFuente, historiaFuente);
        conteosFuente[0] = 99;
        ciclosFuente[0] = 99;
        historiaFuente[0] = 99;
        verificar(vistaAislada.atendidas(PoliticaDisco.FIFO) == 2
                && vistaAislada.ciclos()[0] == 1
                && vistaAislada.desplazamientoAcumulado()[0] == 10,
                "La vista conserva referencias a arreglos del constructor");

        Disco sstf = new Disco(200, 50, 50, 10);
        sstf.cambiarPolitica(new SSTF());
        sstf.encolarSolicitud(4, OperacionES.LEER, 60, 1);
        sstf.avanzarCiclo(1);
        sstf.avanzarCiclo(2);
        VistaMetricasDisco metricasSstf = sstf.consultarMetricas();
        verificar(metricasSstf.atendidas(PoliticaDisco.FIFO) == 0
                && metricasSstf.atendidas(PoliticaDisco.SSTF) == 1
                && sstf.consultarCompletadas()[0].politicaSeleccion() == PoliticaDisco.SSTF,
                "La solicitud atendida no se atribuye a SSTF");
        verificarEvento(sstf.consultarEventos(), "POLITICA_DISCO", 0, 0);
        verificarEvento(sstf.consultarEventos(), "DISCO_ATIENDE", 1, 4);
        verificarEvento(sstf.consultarEventos(), "DISCO_COMPLETA", 2, 4);

        Disco cscan = new Disco(200, 50, 190, 20);
        cscan.cambiarPolitica(new CSCAN());
        cscan.encolarSolicitud(5, OperacionES.LEER, 10, 1);
        cscan.avanzarCiclo(1);
        cscan.avanzarCiclo(2);
        cscan.avanzarCiclo(3);
        VistaMetricasDisco metricasCscan = cscan.consultarMetricas();
        verificar(metricasCscan.desplazamientoTotal() == 19
                && metricasCscan.desplazamientoAcumulado()[0] == 9
                && metricasCscan.desplazamientoAcumulado()[1] == 19,
                "El salto C-SCAN se sumo al desplazamiento real");

        Disco swap = new Disco(200, 50, 150, 10);
        swap.encolarSolicitud(3, OperacionES.SUSPENDER, 150, 1);
        verificar(swap.consultarMetricas().suspensiones() == 0,
                "La suspension se cuenta antes de completarse");
        swap.avanzarCiclo(1);
        verificar(swap.consultarMetricas().suspensiones() == 1,
                "La suspension completada no se cuenta");
        swap.encolarSolicitud(3, OperacionES.REACTIVAR, 151, 1);
        swap.avanzarCiclo(2);
        verificar(swap.consultarMetricas().reactivaciones() == 0,
                "La reactivacion se cuenta antes de completarse");
        swap.avanzarCiclo(3);
        VistaMetricasDisco metricasSwap = swap.consultarMetricas();
        verificar(metricasSwap.suspensiones() == 1 && metricasSwap.reactivaciones() == 1,
                "Las operaciones de swap no actualizan sus metricas");
        System.out.println("OK: desplazamiento, esperas, politicas, swap, historia y bitacora del disco");
    }

    private static void verificarEvento(EventoSistema[] eventos, String tipo, long ciclo, long proceso) {
        for (EventoSistema evento : eventos)
            if (evento.tipo().equals(tipo) && evento.ciclo() == ciclo && evento.proceso() == proceso) return;
        throw new AssertionError("Falta evento " + tipo + " en ciclo " + ciclo + " para P" + proceso);
    }

    private static void verificarArreglo(long[] actual, long[] esperado, String mensaje) {
        verificar(actual.length == esperado.length, mensaje + " (cantidad)");
        for (int i = 0; i < actual.length; i++)
            verificar(actual[i] == esperado[i], mensaje + " en " + i);
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
