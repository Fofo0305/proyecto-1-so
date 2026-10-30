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
    private boolean limpiado;
    private boolean redPreparada;
    private long redHasta = -1;
    private MotivoBloqueo esperaActual = MotivoBloqueo.NINGUNO;
    private long ciclosSemaforo, esperasSemaforo, ciclosRed, esperasRed;

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
        if (limpiado) throw new IllegalStateException("La operacion ya fue cerrada");
        if (etapa == EtapaBuffer.ESPERAR_RECURSO && !recursoReservado && !mutexAdquirido)
            etapa = EtapaBuffer.ESPERAR_MUTEX;
        else if (etapa == EtapaBuffer.ESPERAR_MUTEX && recursoReservado && !mutexAdquirido)
            etapa = EtapaBuffer.MODIFICAR;
        else throw new IllegalStateException("No corresponde ejecutar wait");
    }

    public void conceder(SemaforoSimulado semaforo) {
        if (limpiado) throw new IllegalStateException("Un contexto cerrado no puede recibir permisos");
        if (semaforo == getRecurso() && etapa == EtapaBuffer.ESPERAR_MUTEX && !recursoReservado)
            recursoReservado = true;
        else if (semaforo == buffer.getMutexSimulado() && etapa == EtapaBuffer.MODIFICAR && recursoReservado && !mutexAdquirido)
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
        if (!mutexAdquirido || !recursoReservado || !modificado)
            throw new IllegalStateException("No hay una modificacion protegida para liberar");
        mutexAdquirido = false;
        etapa = EtapaBuffer.SENALAR_RECURSO;
    }

    public void completar() {
        exigir(EtapaBuffer.SENALAR_RECURSO);
        if (mutexAdquirido || !recursoReservado || !modificado)
            throw new IllegalStateException("La operacion no puede publicar su resultado");
        recursoReservado = false;
        modificado = false;
        completados++;
        intervaloRestante = parametros.intervalo();
        etapa = EtapaBuffer.INTERVALO;
        redPreparada = false;
        redHasta = -1;
    }

    /** Una latencia por acceso completo; no adquiere permisos ni avanza PC durante la red. */
    public void prepararRed(long ciclo, int latencia) {
        exigir(EtapaBuffer.ESPERAR_RECURSO);
        if (redPreparada || latencia < 0) throw new IllegalStateException("Acceso remoto ya preparado o latencia invalida");
        redPreparada = true;
        redHasta = latencia == 0 ? -1 : Math.addExact(ciclo, latencia - 1L);
    }

    private void exigir(EtapaBuffer esperada) {
        if (limpiado || etapa != esperada || completados >= parametros.elementos())
            throw new IllegalStateException("Etapa de buffer invalida");
    }

    /**
     * Devuelve los permisos retenidos al abandonar, una sola vez.
     * Si el contenido ya cambio se publica en el semaforo opuesto; nunca se repite
     * ni revierte la insercion/extraccion. El nucleo cancela antes la espera pendiente.
     */
    public SemaforoSimulado[] abandonar() {
        if (limpiado) return new SemaforoSimulado[0];
        cerrarBloqueo();
        SemaforoSimulado[] devolver = new SemaforoSimulado[(mutexAdquirido ? 1 : 0) + (recursoReservado ? 1 : 0)];
        int posicion = 0;
        if (mutexAdquirido) devolver[posicion++] = buffer.getMutexSimulado();
        if (recursoReservado) devolver[posicion] = modificado ? getRecursoLiberado() : getRecurso();
        mutexAdquirido = false;
        recursoReservado = false;
        limpiado = true;
        cancelado = completados < parametros.elementos();
        redPreparada = false;
        redHasta = -1;
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
    public boolean estaLimpiado() { return limpiado; }
    public boolean isRedPreparada() { return redPreparada; }
    public long getRedHasta() { return redHasta; }

    public void iniciarBloqueo(MotivoBloqueo motivo) {
        if (limpiado || esperaActual != MotivoBloqueo.NINGUNO
                || (motivo != MotivoBloqueo.SEMAFORO && motivo != MotivoBloqueo.RED))
            throw new IllegalStateException("No se puede iniciar otra espera");
        buffer.iniciarBloqueo(motivo);
        esperaActual = motivo;
        if (motivo == MotivoBloqueo.SEMAFORO) esperasSemaforo++; else esperasRed++;
    }

    public void contarCicloBloqueado(MotivoBloqueo motivo) {
        if (limpiado || esperaActual != motivo || motivo == MotivoBloqueo.NINGUNO)
            throw new IllegalStateException("El proceso no espera por ese motivo");
        buffer.contarCicloBloqueado(motivo);
        if (motivo == MotivoBloqueo.SEMAFORO) ciclosSemaforo++; else ciclosRed++;
    }

    public void cerrarBloqueo() {
        if (esperaActual == MotivoBloqueo.NINGUNO) return;
        buffer.cerrarBloqueo(esperaActual);
        esperaActual = MotivoBloqueo.NINGUNO;
    }

    public VistaBloqueoProceso consultarBloqueos(long idProceso, String nombre, int computador) {
        return new VistaBloqueoProceso(idProceso, nombre, computador, buffer.getNombre(),
                ciclosSemaforo, esperasSemaforo, ciclosRed, esperasRed, esperaActual);
    }
}
