package PlanificacionDisco;

import Modelo.DireccionDisco;
import Modelo.PoliticaDisco;
import Modelo.ResultadoPlanificacionDisco;

/** Estrategia intercambiable para ordenar la cola unica del disco. @author Adolfo */
public interface PoliticaPlanificacionDisco {
    PoliticaDisco getTipo();

    ResultadoPlanificacionDisco planificar(int posicionCabezal, int totalBloques,
            DireccionDisco direccion, int[] solicitudes);
}
