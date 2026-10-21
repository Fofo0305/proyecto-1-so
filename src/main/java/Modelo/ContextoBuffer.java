package Modelo;

/** Contexto persistente por proceso; su computador serializa los cambios. @author Luciano Rojas */
public final class ContextoBuffer {
    public record Elemento(long productor, int numero) { }
    private final Buffer<Object> buffer;
    private final ParametrosBuffer parametros;
    private final boolean productor;
    private EtapaBuffer etapa = EtapaBuffer.INTERVALO;
    private int intervaloRestante;
    private int completados;
    private boolean recursoReservado;
    private boolean mutexAdquirido;
    private boolean modificado;
    private boolean cancelado;

    public ContextoBuffer(Buffer<Object> buffer, ParametrosBuffer parametros, TipoProceso tipo) {
        if (buffer == null || parametros == null || !buffer.getNombre().equals(parametros.buffer())
                || (tipo != TipoProceso.PRODUCTOR && tipo != TipoProceso.CONSUMIDOR))
            throw new IllegalArgumentException("Contexto de buffer invalido");
        this.buffer = buffer;
        this.parametros = parametros;
        productor = tipo == TipoProceso.PRODUCTOR;
        intervaloRestante = parametros.intervalo();
    }

    public void avanzarIntervalo() {
        exigir(EtapaBuffer.INTERVALO);
        if (--intervaloRestante == 0) etapa = EtapaBuffer.ESPERAR_RECURSO;
    }

    /** La siguiente etapa queda guardada antes de que el wait libere la CPU. */
    public void iniciarEspera() {
        if (etapa == EtapaBuffer.ESPERAR_RECURSO) etapa = EtapaBuffer.ESPERAR_MUTEX;
        else if (etapa == EtapaBuffer.ESPERAR_MUTEX) etapa = EtapaBuffer.MODIFICAR;
        else throw new IllegalStateException("No corresponde ejecutar wait");
    }

    public void conceder(SemaforoSimulado semaforo) {
        if (semaforo == getRecurso() && etapa == EtapaBuffer.ESPERAR_MUTEX && !recursoReservado)
            recursoReservado = true;
        else if (semaforo == buffer.getMutexSimulado() && etapa == EtapaBuffer.MODIFICAR && !mutexAdquirido)
            mutexAdquirido = true;
        else throw new IllegalStateException("Permiso inesperado en la operacion de buffer");
    }

    public void modificar(long idProceso) {
        exigir(EtapaBuffer.MODIFICAR);
        if (!mutexAdquirido || !recursoReservado) throw new IllegalStateException("Operacion sin permisos");
        if (productor) {
            if (!buffer.insertar(new Elemento(idProceso, completados + 1)))
                throw new IllegalStateException("Buffer lleno pese al permiso de vacios");
        } else if (buffer.extraer() == null) {
            throw new IllegalStateException("Buffer vacio pese al permiso de llenos");
        }
        modificado = true;
        etapa = EtapaBuffer.SENALAR_MUTEX;
    }

    public void liberarMutex() {
        exigir(EtapaBuffer.SENALAR_MUTEX);
        mutexAdquirido = false;
        etapa = EtapaBuffer.SENALAR_RECURSO;
    }

    public void completar() {
        exigir(EtapaBuffer.SENALAR_RECURSO);
        recursoReservado = false;
        modificado = false;
        completados++;
        intervaloRestante = parametros.intervalo();
        etapa = EtapaBuffer.INTERVALO;
    }

    private void exigir(EtapaBuffer esperada) {
        if (cancelado || etapa != esperada || completados >= parametros.elementos())
            throw new IllegalStateException("Etapa de buffer invalida");
    }

    /**
     * Devuelve los permisos retenidos al abandonar, una sola vez.
     * Si el contenido ya cambio se publica en el semaforo opuesto; nunca se repite
     * ni revierte la insercion/extraccion. El nucleo cancela antes la espera pendiente.
     */
    public SemaforoSimulado[] abandonar() {
        if (cancelado) return new SemaforoSimulado[0];
        SemaforoSimulado[] devolver = new SemaforoSimulado[(mutexAdquirido ? 1 : 0) + (recursoReservado ? 1 : 0)];
        int posicion = 0;
        if (mutexAdquirido) devolver[posicion++] = buffer.getMutexSimulado();
        if (recursoReservado) devolver[posicion] = modificado ? getRecursoLiberado() : getRecurso();
        mutexAdquirido = false;
        recursoReservado = false;
        cancelado = true;
        return devolver;
    }

    public SemaforoSimulado getRecurso() { return productor ? buffer.getVacios() : buffer.getLlenos(); }
    public SemaforoSimulado getRecursoLiberado() { return productor ? buffer.getLlenos() : buffer.getVacios(); }
    public Buffer<Object> getBuffer() { return buffer; }
    public EtapaBuffer getEtapa() { return etapa; }
    public int getCompletados() { return completados; }
    public boolean tieneRecurso() { return recursoReservado; }
    public boolean tieneMutex() { return mutexAdquirido; }
    public boolean estaModificado() { return modificado; }
    public boolean estaCancelado() { return cancelado; }
}
