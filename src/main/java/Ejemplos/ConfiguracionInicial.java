package Ejemplos;

import Modelo.CargaInicial;
import Modelo.Configuracion;
import Modelo.DefinicionProceso;

/** Escenario reproducible para observar admision, espera de RAM y FCFS. @author Adolfo */
public final class ConfiguracionInicial {
    private ConfiguracionInicial() { }

    public static CargaInicial crear() {
        return new CargaInicial(new Configuracion(new int[] {96}, 700, 4),
                new DefinicionProceso[] {
                    new DefinicionProceso("Compilar", 1, 1, 32, 8, 80),
                    new DefinicionProceso("Exportar", 1, 2, 24, 5, 80),
                    new DefinicionProceso("Informe", 1, 0, 64, 4, 80)
                });
    }
}
