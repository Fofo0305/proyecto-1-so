package com.mycompany.proyecto1so2526.contratos;

import com.mycompany.proyecto1so2526.estructuras.Cola;
import com.mycompany.proyecto1so2526.modelo.Proceso;

/** La política selecciona; no ejecuta instrucciones. El núcleo protege la decisión completa.
 * @author Adolfo
 */
public interface Politica {
    Proceso seleccionar(Cola<Proceso> listos);
    boolean debeInterrumpir(Proceso actual, Cola<Proceso> listos, int ciclosEjecutados);
}
