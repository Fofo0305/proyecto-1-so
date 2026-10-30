package Modelo;

/** Proxima instruccion de una operacion de productor consumidor. @author Luciano Rojas */
public enum EtapaBuffer {
    INTERVALO, ESPERAR_RECURSO, ESPERAR_MUTEX, MODIFICAR, SENALAR_MUTEX, SENALAR_RECURSO
}
