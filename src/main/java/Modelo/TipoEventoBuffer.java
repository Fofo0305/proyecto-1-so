package Modelo;

/** Sucesos del motor de productor consumidor. @author Luciano Rojas */
public enum TipoEventoBuffer {
    PRODUCCION, CONSUMO, BLOQUEO_SEMAFORO, DESPERTAR_SEMAFORO,
    ACCESO_REMOTO, BLOQUEO_RED, DESPERTAR_RED, DEVOLUCION_PERMISO, TERMINACION
}
